package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.model.metaColumn.FlatMetaColumn;
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
public class MetaColumnRepository {
    private final JdbcTemplate jdbcTemplate;

    public MetaColumnRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public class FlatMetaColumnRowMapper implements RowMapper<FlatMetaColumn> {

        @Override
        public FlatMetaColumn mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatMetaColumn fi = new FlatMetaColumn();

            fi.setId(rs.getString("id"));
            fi.setName(rs.getString("name"));
            fi.setDescription(rs.getString("description"));
            fi.setColumnType(rs.getString("column_type"));
            fi.setMetaObjectId(UUID.fromString(rs.getString("meta_object_id")));
            fi.setMetaObjectName(rs.getString("meta_object_name"));
            fi.setIsKey(rs.getBoolean("is_key"));
            fi.setSchemaName(rs.getString("schema_name"));
            fi.setModified(rs.getTimestamp("modified").toLocalDateTime());
            fi.setMetaDatabaseId(UUID.fromString(rs.getString("meta_database_id")));
            fi.setVersionId(rs.getInt("version_id"));

            return fi;
        }
    }

    public FlatMetaColumn createMetaColumn(FlatMetaColumn col, UserDetails userDetails) {
        Timestamp ts = new Timestamp(new Date().getTime());
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".meta_column (id, name, description, " +
                "created, creator, modified, modifier, meta_object_id, is_key, column_type, meta_database_id) VALUES (" +
                "?,?,?,?,?,?,?,?,?,?,?)", UUID.fromString(col.getId()), col.getName(), col.getDescription(), ts, userDetails.getUid(),
                ts, userDetails.getUid(), col.getMetaObjectId(), col.getIsKey(), col.getColumnType(), col.getMetaDatabaseId());
        return col;
    }

    public SearchResponse<FlatMetaColumn> searchMetaColumns(SearchRequestWithJoin searchRequest,
                                                            SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "select mc.*, mo.name AS meta_object_name, sch.name AS schema_name, meta_database.system_id AS system_id, meta_database.version_id AS version_id "
                + " from da_"
                + userDetails.getTenant() + ".meta_column mc JOIN da_" +
                userDetails.getTenant() + ".meta_object mo ON mc.meta_object_id=mo.id JOIN da_" +
                userDetails.getTenant() + ".meta_object sch ON mo.parent_id=sch.id JOIN da_" +
                userDetails.getTenant() + ".meta_database ON mo.meta_database_id=meta_database.id AND mo.version_id=meta_database.version_id AND meta_database.state='PUBLISHED' ";


        log.info("SQ " + subQuery);

        subQuery = "SELECT sq.*, t.tags FROM (" + subQuery + ") as sq left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=sq.id ";

        String queryForItems = "SELECT distinct tbl1.* FROM (" + subQuery + ") tbl1 " + join
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();



        List<FlatMetaColumn> flatItems = jdbcTemplate.query(queryForItems, new FlatMetaColumnRowMapper(),
                whereValues.toArray());

        for (FlatMetaColumn mc : flatItems) {
            jdbcTemplate.query("SELECT ea.entity_id, e.name || '.' || ea.name AS name FROM da_" + userDetails.getTenant() + ".entity_attribute ea"
                            + " JOIN da_" + userDetails.getTenant() + ".entity e ON ea.entity_id=e.id"
                            + " JOIN da_" + userDetails.getTenant() + ".entity_attribute_to_sample_property ea2sp ON ea2sp.entity_attribute_id=ea.id"
                            + " JOIN da_" + userDetails.getTenant() + ".reference r ON r.source_id=ea2sp.entity_sample_property_id AND r.reference_type='SAMPLE_PROPERTY_TO_META_COLUMN'"
                            + " WHERE r.target_id=? LIMIT 1",
                    new RowCallbackHandler() {
                        @Override
                        public void processRow(ResultSet rs) throws SQLException {
                            mc.setEntityAttributeName(rs.getString("name"));
                            mc.setEntityId(UUID.fromString(rs.getString("entity_id")));
                        }
                    }
                    , UUID.fromString(mc.getId()));
        }

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") tbl1 " + join
                + where;
        Long total = jdbcTemplate.queryForObject(queryForTotal, Long.class, whereValues.toArray());

        SearchResponse<FlatMetaColumn> res = new SearchResponse<>(total.intValue(), searchRequest.getLimit(),
                searchRequest.getOffset(), flatItems);

        return res;
    }

    public FlatMetaColumn findMetaColumn(FlatMetaColumn fmc, UserDetails userDetails) {
        try {
            Integer versionId = jdbcTemplate.queryForObject("SELECT version_id FROM da_" + userDetails.getTenant()
                + ".meta_database WHERE id=? AND state='PUBLISHED'", Integer.class, fmc.getMetaDatabaseId());
            return jdbcTemplate.queryForObject("select mc.*, mo.name AS meta_object_name, sch.name AS schema_name, " + versionId + " AS version_id from da_" + userDetails.getTenant() + ".meta_column mc LEFT JOIN da_" +
                            userDetails.getTenant() + ".meta_object mo ON mc.meta_object_id=mo.id LEFT JOIN da_" +
                            userDetails.getTenant() + ".meta_object sch ON mo.parent_id=sch.id WHERE NOT mc.is_deleted AND NOT mo.is_deleted AND NOT sch.is_deleted AND " +
                            "mc.name=? AND mc.meta_object_id=(SELECT id FROM da_"
                            + userDetails.getTenant() + ".meta_object WHERE meta_database_id=? AND name=? AND version_id=? AND parent_id=(SELECT id FROM da_"
                            + userDetails.getTenant() + ".meta_object WHERE meta_database_id=? AND name=? AND version_id=?))",
                    new FlatMetaColumnRowMapper(), fmc.getName(), fmc.getMetaDatabaseId(),
                    fmc.getMetaObjectName(), versionId, fmc.getMetaDatabaseId(), fmc.getSchemaName(), versionId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<FlatMetaColumn> listMetaColumns(FlatMetaDatabase fmb, UserDetails userDetails) {
        return jdbcTemplate.query("select mc.*, mo.name AS meta_object_name, sch.name AS schema_name, " + fmb.getVersionId() + " as version_id from da_" + userDetails.getTenant() + ".meta_column mc LEFT JOIN da_" +
                userDetails.getTenant() + ".meta_object mo ON mc.meta_object_id=mo.id LEFT JOIN da_" +
                userDetails.getTenant() + ".meta_object sch ON mo.parent_id=sch.id WHERE NOT mc.is_deleted AND NOT mo.is_deleted AND NOT sch.is_deleted AND" +
                " mc.meta_database_id=? AND mc.meta_object_id IN (SELECT id FROM da_" + userDetails.getTenant()
                + ".meta_object WHERE meta_database_id=? AND version_id=?)", new FlatMetaColumnRowMapper(),
                UUID.fromString(fmb.getId()), UUID.fromString(fmb.getId()), fmb.getVersionId());
    }
}
