package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.model.metaDatabase.FlatMetaDatabase;
import ru.bssg.lottabyte.core.model.metaObject.FlatMetaObject;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static ru.bssg.lottabyte.coreapi.util.QueryHelper.getSearchSQLParts;

@Repository
@Slf4j
public class MetaObjectRepository {
    private final JdbcTemplate jdbcTemplate;

    public MetaObjectRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public class FlatMetaObjectRowMapper implements RowMapper<FlatMetaObject> {

        @Override
        public FlatMetaObject mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatMetaObject fi = new FlatMetaObject();
            fi.setId(rs.getString("id"));
            fi.setName(rs.getString("name"));
            fi.setDescription(rs.getString("description"));
            fi.setModified(rs.getTimestamp("modified").toLocalDateTime());
            if (rs.getString("parent_id") != null)
                fi.setParentId(UUID.fromString(rs.getString("parent_id")));
            fi.setParentName(rs.getString("parent_name"));
            fi.setMetaDatabaseId(UUID.fromString(rs.getString("meta_database_id")));
            fi.setMetaObjectType(rs.getString("meta_object_type"));
            fi.setVersionId(rs.getInt("version_id"));
            return fi;
        }
    }

    public FlatMetaObject createMetaObject(FlatMetaObject fmo, UserDetails userDetails) {
        Timestamp ts = new Timestamp(new Date().getTime());
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".meta_object (id, name, description, " +
                "created, creator, modified, modifier, meta_database_id, parent_id, version_id, meta_object_type) VALUES" +
                "(?,?,?,?,?,?,?,?,?,?,?)", UUID.fromString(fmo.getId()), fmo.getName(), fmo.getDescription(), ts, userDetails.getUid(),
                ts, userDetails.getUid(), fmo.getMetaDatabaseId(), fmo.getParentId(), fmo.getVersionId(), fmo.getMetaObjectType());
        return fmo;
    }

    public SearchResponse<FlatMetaObject> searchMetaObjects(SearchRequestWithJoin searchRequest,
                                                               SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "select mo.*, p.name AS parent_name from da_" + userDetails.getTenant() + ".meta_object mo LEFT JOIN da_" +
                userDetails.getTenant() + ".meta_object p ON mo.parent_id=p.id";
        /*if (userDetails.getStewardId() != null && searchRequest.getLimitSteward() != null
                && searchRequest.getLimitSteward()) {
            subQuery = subQuery + QueryHelper.getWhereIdInQuery(ArtifactType.business_entity, userDetails);
        }*/

        subQuery = "SELECT sq.*, t.tags FROM (" + subQuery + ") as sq left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=sq.id ";

        String queryForItems = "SELECT distinct tbl1.* FROM (" + subQuery + ") tbl1 " + join
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatMetaObject> flatItems = jdbcTemplate.query(queryForItems, new FlatMetaObjectRowMapper(),
                whereValues.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") tbl1 " + join
                + where;
        Long total = jdbcTemplate.queryForObject(queryForTotal, Long.class, whereValues.toArray());

        SearchResponse<FlatMetaObject> res = new SearchResponse<>(total.intValue(), searchRequest.getLimit(),
                searchRequest.getOffset(), flatItems);

        return res;
    }

    public FlatMetaObject findMetaObject(FlatMetaObject fmo, UserDetails userDetails) {
        try {
            if (fmo.getMetaObjectType().equals("SCHEMA"))
                return jdbcTemplate.queryForObject("select mo.*, p.name AS parent_name from da_" + userDetails.getTenant()
                        + ".meta_object mo LEFT JOIN da_" + userDetails.getTenant() + ".meta_object p ON mo.parent_id=p.id " +
                        "WHERE mo.meta_database_id=? AND mo.version_id=(SELECT version_id FROM da_"
                        + userDetails.getTenant() + ".meta_database WHERE id=? AND state='PUBLISHED') AND mo.meta_object_type='SCHEMA' AND mo.name=?", new FlatMetaObjectRowMapper(),
                        fmo.getMetaDatabaseId(), fmo.getMetaDatabaseId(), fmo.getName());
            else
                return jdbcTemplate.queryForObject("select mo.*, p.name AS parent_name from da_" + userDetails.getTenant()
                        + ".meta_object mo LEFT JOIN da_" + userDetails.getTenant() + ".meta_object p ON mo.parent_id=p.id " +
                        "WHERE mo.meta_database_id=? AND mo.version_id=(SELECT version_id FROM da_"
                        + userDetails.getTenant() + ".meta_database WHERE id=? AND state='PUBLISHED') AND mo.meta_object_type='" +
                        fmo.getMetaObjectType() + "' AND mo.name=? AND p.name=?", new FlatMetaObjectRowMapper(),
                        fmo.getMetaDatabaseId(), fmo.getMetaDatabaseId(), fmo.getName(), fmo.getParentName());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }

    }

    public List<FlatMetaObject> listMetaObjects(FlatMetaDatabase fmb, UserDetails userDetails) {
        return jdbcTemplate.query("select mo.*, p.name AS parent_name from da_" + userDetails.getTenant()
                + ".meta_object mo LEFT JOIN da_" + userDetails.getTenant() + ".meta_object p ON mo.parent_id=p.id " +
                "WHERE mo.meta_database_id=? AND mo.version_id=? AND NOT mo.is_deleted", new FlatMetaObjectRowMapper(),
                UUID.fromString(fmb.getId()), fmb.getVersionId());
    }

    public FlatMetaObject getById(UUID id, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT d.* FROM da_" + userDetails.getTenant()
                + ".meta_object o WHERE o.id=?", new FlatMetaObjectRowMapper(), id);
    }
}
