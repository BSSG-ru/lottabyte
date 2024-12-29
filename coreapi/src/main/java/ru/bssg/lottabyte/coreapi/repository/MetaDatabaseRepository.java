package ru.bssg.lottabyte.coreapi.repository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.dal.FlatItemRowMapper;
import ru.bssg.lottabyte.core.model.metaDatabase.FlatMetaDatabase;
import ru.bssg.lottabyte.core.model.relation.Relation;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static ru.bssg.lottabyte.coreapi.util.QueryHelper.getSearchSQLParts;

@Repository
@Slf4j
@AllArgsConstructor
public class MetaDatabaseRepository {
    private final JdbcTemplate jdbcTemplate;

    public static class FlatMetaDatabaseRowMapper extends FlatItemRowMapper<FlatMetaDatabase> {
        public FlatMetaDatabaseRowMapper() { super(FlatMetaDatabase::new); }

        @Override
        public FlatMetaDatabase mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatMetaDatabase fi = super.mapRow(rs, rowNum);
            fi.setJdbcUrl(rs.getString("jdbc_url"));
            fi.setDriverClassName(rs.getString("driver_class_name"));
            if (rs.getString("system_id") != null)
                fi.setSystemId(UUID.fromString(rs.getString("system_id")));
            fi.setState(rs.getString("state"));
            return fi;
        }
    }

    public FlatMetaDatabase getById(UUID id, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT d.* FROM da_" + userDetails.getTenant()
                + ".meta_database d WHERE d.id=? AND state='PUBLISHED'", new FlatMetaDatabaseRowMapper(), id);
    }

    public FlatMetaDatabase getById(UUID id, Integer versionId, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT d.* FROM da_" + userDetails.getTenant()
                + ".meta_database d WHERE d.id=? AND version_id=?", new FlatMetaDatabaseRowMapper(), id, versionId);
    }

    public FlatMetaDatabase getVersionById(UUID id, Integer versionId, UserDetails userDetails) {
        FlatMetaDatabase fmb = null;
        try {
            fmb = jdbcTemplate.queryForObject("SELECT d.* FROM da_" + userDetails.getTenant()
                    + ".meta_database d WHERE d.id=? AND d.version_id=?", new FlatMetaDatabaseRowMapper(),
                    id, versionId);
        } catch (EmptyResultDataAccessException e) {}
        if (fmb == null) {
            fmb = jdbcTemplate.queryForObject("SELECT d.* FROM da_" + userDetails.getTenant()
                            + ".meta_database_hist d WHERE d.id=? AND d.version_id=?", new FlatMetaDatabaseRowMapper(),
                    id, versionId);
        }
        //fmb.setVersionId(versionId);
        return fmb;
    }

    public FlatMetaDatabase createMetaDatabase(FlatMetaDatabase fmb, UserDetails userDetails) {
        Timestamp ts = new Timestamp(new Date().getTime());
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".meta_database (id, name, description, driver_class_name," +
                "jdbc_url, history_start, history_end, created, creator, modified, modifier, system_id, state, version_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,'PUBLISHED',?)",
                UUID.fromString(fmb.getId()), fmb.getName(), fmb.getDescription(), fmb.getDriverClassName(), fmb.getJdbcUrl(),
                ts, ts, ts, userDetails.getUid(), ts, userDetails.getUid(), fmb.getSystemId(), fmb.getVersionId());
        return fmb;
    }

    public SearchResponse<FlatMetaDatabase> searchMetaDatabases(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "SELECT d.* FROM da_" + userDetails.getTenant() + ".meta_database d ";

        subQuery = "SELECT sq.* FROM (" + subQuery + ") as sq ";
        subQuery = "SELECT distinct sq.*, t.tags FROM (" + subQuery + ") as sq "
                + "left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_" + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant() + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=sq.id ";

        List<FlatMetaDatabase> flatItems =
                jdbcTemplate.query("SELECT * FROM (" + subQuery + ") as tbl1 " + where
                        + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                        + searchRequest.getLimit(), new FlatMetaDatabaseRowMapper(), whereValues.toArray());

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(distinct id) FROM (" + subQuery + ") as tbl1 " + where, Integer.class, whereValues.toArray());

        SearchResponse<FlatMetaDatabase> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public FlatMetaDatabase findMetaDatabaseByUrl(String jdbc_url, UserDetails userDetails) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM da_" + userDetails.getTenant() + ".meta_database WHERE " +
                    "jdbc_url=? AND state='PUBLISHED' ORDER BY modified DESC LIMIT 1", new FlatMetaDatabaseRowMapper(), jdbc_url);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public FlatMetaDatabase updateMetaDatabase(FlatMetaDatabase fmb, UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (fmb.getName() != null) {
            sets.add("name=?");
            args.add(fmb.getName());
        }
        if (fmb.getDescription() != null) {
            sets.add("description=?");
            args.add(fmb.getDescription());
        }
        if (fmb.getDriverClassName() != null) {
            sets.add("driver_class_name=?");
            args.add(fmb.getDriverClassName());
        }
        if (fmb.getJdbcUrl() != null) {
            sets.add("jdbc_url=?");
            args.add(fmb.getJdbcUrl());
        }
        if (fmb.getSystemId() != null) {
            sets.add("system_id=?");
            args.add(fmb.getSystemId());
        }
        if (fmb.getVersionId() != null) {
            sets.add("version_id=?");
            args.add(fmb.getVersionId());
        }
        if (fmb.getState() != null) {
            sets.add("state=?");
            args.add(fmb.getState());
        }
        if (sets.size() > 0) {
            Timestamp ts = new Timestamp(new Date().getTime());

            sets.add("modified=?");
            sets.add("modifier=?");
            args.add(ts);
            args.add(userDetails.getUid());
            args.add(UUID.fromString(fmb.getId()));
            args.add(fmb.getVersionId());

            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".meta_database SET " + StringUtils.join(sets, ", ")
                    + " WHERE id=? AND version_id=?", args.toArray());
        }

        return getById(UUID.fromString(fmb.getId()), fmb.getVersionId(), userDetails);
    }

    public List<FlatMetaDatabase> getVersionsById(String id, String url, UserDetails userDetails) {
        /*String subQuery = " select h.*, pu.display_name as modifier_display_name, pu.email as modifier_email, pu.description as modifier_description from da_"
                + userDetails.getTenant() + ".meta_database_hist h"
                + " left join usermgmt.platform_users pu ON CAST(pu.uid AS TEXT)=h.modifier ";*/

        String subQuery = "SELECT h.*, pu.display_name as modifier_display_name, pu.email as modifier_email, pu.description as modifier_description from da_" +
                userDetails.getTenant() + ".meta_database h left join usermgmt.platform_users pu ON CAST(pu.uid AS TEXT)=h.modifier WHERE h.version_id>0 and h.state='DRAFT_HISTORY'";

        return jdbcTemplate.query(
                "SELECT * FROM (" + subQuery + ") sq WHERE id = ? order by version_id desc",
                new FlatMetaDatabaseRowMapper(), UUID.fromString(id));
    }

    public List<Relation> getMetaDatabaseTasks(UUID id, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT t.id, t.name FROM da_" + userDetails.getTenant() + ".reference r JOIN da_" + userDetails.getTenant() +
                ".task t ON r.target_id=t.id WHERE r.source_id=? AND r.reference_type='META_DATABASE_TO_TASK'",
                new RowMapper<Relation>() {
                    @Override
                    public Relation mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new Relation(rs.getString("id"), rs.getString("name"));
                    }
                },
                id
        );
    }
}
