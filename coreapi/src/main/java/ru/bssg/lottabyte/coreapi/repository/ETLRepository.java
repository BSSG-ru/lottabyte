package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactState;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.WorkflowableMetadata;
import ru.bssg.lottabyte.core.model.etl.ETL;
import ru.bssg.lottabyte.core.model.etl.ETLEntity;
import ru.bssg.lottabyte.core.model.etl.FlatETL;
import ru.bssg.lottabyte.core.model.etl.UpdatableETLEntity;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.util.QueryHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ru.bssg.lottabyte.coreapi.util.QueryHelper.getSearchSQLParts;

@Repository
@Slf4j
public class ETLRepository extends WorkflowableRepository<ETL> {

    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = { "code", "algorithm", "etl_type_id", "system_id" };

    public static class ETLRowMapper implements RowMapper<ETL> {

        @Override
        public ETL mapRow(ResultSet rs, int rowNum) throws SQLException {
            ETLEntity e = new ETLEntity();
            e.setId(rs.getString("id"));
            e.setName(rs.getString("name"));
            e.setDescription(rs.getString("description"));
            e.setShortDescription(rs.getString("short_description"));
            e.setCode(rs.getString("code"));
            e.setAlgorithm(rs.getString("algorithm"));
            e.setSystemId(rs.getString("system_id"));
            e.setEtlTypeId(rs.getString("etl_type_id"));
            return new ETL(e, new WorkflowableMetadata(rs, e.getArtifactType()));
        }
    }

    public static class FlatETLRowMapper implements RowMapper<FlatETL> {
        @Override
        public FlatETL mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatETL etl = new FlatETL();
            etl.setId(rs.getString("id"));
            etl.setName(rs.getString("name"));
            etl.setDescription(rs.getString("description"));
            etl.setShortDescription(rs.getString("short_description"));
            etl.setVersionId(rs.getInt("version_id"));
            etl.setModified(rs.getTimestamp("modified").toLocalDateTime());
            etl.setCode(rs.getString("code"));
            etl.setAlgorithm(rs.getString("algorithm"));
            etl.setSystemId(rs.getString("system_id"));
            etl.setSystemName(rs.getString("system_name"));
            etl.setEtlTypeId(rs.getString("etl_type_id"));
            etl.setEtlTypeName(rs.getString("etl_type_name"));
            etl.setState(ArtifactState.valueOf(rs.getString("state")));
            etl.setWorkflowTaskId(rs.getString("workflow_task_id"));
            return etl;
        }
    }

    class ETLTypeRowMapper implements RowMapper<ETLType> {
        @Override
        public ETLType mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ETLType(rs.getString("id"), rs.getString("name"),
                    rs.getString("description"));
        }
    }

    public ETLRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.etl.name(), extFields);
        super.setMapper(new ETLRepository.ETLRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    public String createETL(UpdatableETLEntity newETLEntity, String workflowTaskId,
                            UserDetails userDetails) {
        UUID newId = newETLEntity.getId() != null ? UUID.fromString(newETLEntity.getId())
                : UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());

        String query = "INSERT INTO da_" + userDetails.getTenant() + ".\"etl\" " +
                "(id, \"name\", description, short_description, code, algorithm, system_id, etl_type_id, state, workflow_task_id, created, creator, modified, modifier) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(query, newId, newETLEntity.getName(), newETLEntity.getDescription(),
                newETLEntity.getShortDescription(), newETLEntity.getCode(), newETLEntity.getAlgorithm(),
                newETLEntity.getSystemId() == null ? null : UUID.fromString(newETLEntity.getSystemId()),
                newETLEntity.getEtlTypeId() == null ? null : UUID.fromString(newETLEntity.getEtlTypeId()),
                ArtifactState.DRAFT.toString(), workflowTaskId != null ? UUID.fromString(workflowTaskId) : null,
                ts, userDetails.getUid(), ts, userDetails.getUid());
        return newId.toString();
    }

    public void patchETL(String etlId, UpdatableETLEntity etlEntity, boolean updateNulls, UserDetails userDetails)
            throws LottabyteException {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".\"etl\" SET modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));
        if (updateNulls || etlEntity.getName() != null) {
            sets.add("\"name\" = ?");
            params.add(etlEntity.getName());
        }
        if (updateNulls || etlEntity.getDescription() != null) {
            sets.add("description = ?");
            params.add(etlEntity.getDescription());
        }
        if (updateNulls || etlEntity.getShortDescription() != null) {
            sets.add("short_description = ?");
            params.add(etlEntity.getShortDescription());
        }
        if (updateNulls || etlEntity.getCode() != null) {
            sets.add("code = ?");
            params.add(etlEntity.getCode());
        }
        if (updateNulls || etlEntity.getAlgorithm() != null) {
            sets.add("algorithm = ?");
            params.add(etlEntity.getAlgorithm());
        }

        if (updateNulls || etlEntity.getSystemId() != null) {
            sets.add("system_id = ?");
            params.add((etlEntity.getSystemId() == null || etlEntity.getSystemId().isEmpty()) ? null : UUID.fromString(etlEntity.getSystemId()));
        }
        if (updateNulls || etlEntity.getEtlTypeId() != null) {
            sets.add("etl_type_id = ?");
            params.add((etlEntity.getEtlTypeId() == null || etlEntity.getEtlTypeId().isEmpty()) ? null : UUID.fromString(etlEntity.getEtlTypeId()));
        }

        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(etlId));
            jdbcTemplate.update(query, params.toArray());
        }
    }

    public SearchResponse<FlatETL> searchETL(SearchRequestWithJoin searchRequest,
                                                          SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, true, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "select * from da_" + userDetails.getTenant() + ".etl ";
        if (userDetails.getStewardId() != null && searchRequest.getLimitSteward() != null
                && searchRequest.getLimitSteward()) {
            subQuery = subQuery + QueryHelper.getWhereIdInQuery(ArtifactType.etl, userDetails);
        }

        subQuery = "SELECT sq.*, wft.workflow_state, t.tags FROM (" + subQuery + ") as sq left join da_"
                + userDetails.getTenant() + ".workflow_task wft "
                + " on sq.workflow_task_id = wft.id "
                + "left join (select e2t.artifact_id, string_agg(t.name, ',') as tags from da_"
                + userDetails.getTenant() + ".tag t join da_" + userDetails.getTenant()
                + ".tag_to_artifact e2t on e2t.tag_id=t.id group by e2t.artifact_id) t on t.artifact_id=sq.id ";

        String queryForItems = "SELECT distinct tbl1.*, system.name AS system_name, etl_type.name AS etl_type_name FROM ("
                + subQuery + ") tbl1 " + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                + " LEFT JOIN da_" + userDetails.getTenant()
                + ".etl_type etl_type on tbl1.etl_type_id=etl_type.id "
                + where + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                + searchRequest.getLimit();

        List<FlatETL> flatItems = jdbcTemplate.query(queryForItems, new ETLRepository.FlatETLRowMapper(), whereValues.toArray());

        String queryForTotal = "SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") tbl1 "
                + join
                + " LEFT JOIN da_" + userDetails.getTenant() + ".system system ON tbl1.system_id=system.id"
                + " LEFT JOIN da_" + userDetails.getTenant()
                + ".etl_type etl_type on tbl1.etl_type_id=etl_type.id "
                + where;
        Integer total = jdbcTemplate.queryForObject(queryForTotal, Integer.class, whereValues.toArray());

        SearchResponse<FlatETL> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public List<ETLType> getETLTypes(UserDetails userDetails) {
        return jdbcTemplate.query("SELECT id, name, description FROM da_" + userDetails.getTenant() +
                ".etl_type", new ETLRepository.ETLTypeRowMapper());
    }

    public ETLType getETLTypeById(String id, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT id, name, description FROM da_" + userDetails.getTenant() +
                ".etl_type WHERE id=?", new ETLRepository.ETLTypeRowMapper(), UUID.fromString(id));
    }
}
