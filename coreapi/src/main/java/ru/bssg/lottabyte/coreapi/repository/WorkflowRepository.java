package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.domain.DomainEntity;
import ru.bssg.lottabyte.core.model.domain.FlatDomain;
import ru.bssg.lottabyte.core.model.domain.UpdatableDomainEntity;
import ru.bssg.lottabyte.core.model.workflow.*;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class WorkflowRepository extends GenericArtifactRepository<Workflow> {

    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = {};

    @Autowired
    public WorkflowRepository (JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.workflow.name(), extFields);
        super.setMapper(new WorkflowRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    private class WorkflowRowMapper implements RowMapper<Workflow> {
        @Override
        public Workflow mapRow(ResultSet rs, int rowNum) throws SQLException {
            WorkflowEntity e = new WorkflowEntity();
            e.setName(rs.getString("name"));
            e.setDescription(rs.getString("description"));
            e.setWorkflowType(WorkflowType.valueOf(rs.getString("workflow_type")));
            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));
            return new Workflow(e, md);
        }
    }

    private class WorkflowTaskRowMapper implements RowMapper<WorkflowTask> {
        @Override
        public WorkflowTask mapRow(ResultSet rs, int rowNum) throws SQLException {
            WorkflowTaskEntity e = new WorkflowTaskEntity();
            e.setArtifactId(rs.getString("artifact_id"));
            e.setArtifactType(ArtifactType.fromString(rs.getString("artifact_type")));
            e.setWorkflowState(rs.getString("workflow_state"));
            e.setWorkflowId(rs.getString("workflow_id"));
            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));
            return new WorkflowTask(e, md);
        }
    }

    private class WorkflowProcessDefinitionMapper implements RowMapper<WorkflowProcessDefinition> {
        @Override
        public WorkflowProcessDefinition mapRow(ResultSet rs, int rowNum) throws SQLException {
            WorkflowProcessDefinition pd = new WorkflowProcessDefinition();
            pd.setArtifactType(ArtifactType.fromString(rs.getString("artifact_type")));
            pd.setArtifactAction(ArtifactAction.valueOf(rs.getString("artifact_action")));
            pd.setProcessDefinitionKey(rs.getString("process_definition_key"));
            pd.setDescription(rs.getString("description"));
            pd.setCreatedBy(rs.getString("creator"));
            pd.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            pd.setModifiedBy(rs.getString("modifier"));
            pd.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            pd.setId(rs.getString("id"));
            return pd;
        }
    }

    private class FlatWorkflowProcessDefinitionRowMapper implements RowMapper<FlatWorkflowProcessDefinition> {
        @Override
        public FlatWorkflowProcessDefinition mapRow(ResultSet rs, int rowNum) throws SQLException {
            FlatWorkflowProcessDefinition pd = new FlatWorkflowProcessDefinition();
            pd.setArtifactType(ArtifactType.fromString(rs.getString("artifact_type")));
            pd.setArtifactAction(ArtifactAction.valueOf(rs.getString("artifact_action")));
            pd.setProcessDefinitionKey(rs.getString("process_definition_key"));
            pd.setDescription(rs.getString("description"));
            pd.setCreatedBy(rs.getString("creator"));
            pd.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            pd.setModifiedBy(rs.getString("modifier"));
            pd.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            pd.setId(rs.getString("id"));
            pd.setArtifactTypeName(rs.getString("artifact_type_name"));
            return pd;
        }
    }

    public WorkflowTask getWorkflowTaskById(String workflowTaskId, UserDetails userDetails) {
        if (workflowTaskId == null || workflowTaskId.isEmpty())
            return null;

        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".workflow_task WHERE id = ?",
                new WorkflowTaskRowMapper(), UUID.fromString(workflowTaskId))
                .stream().findFirst().orElse(null);
    }

    public String createWorkflowTask(String workflowTaskId, String artifactId, ArtifactType artifactType, String workflowId, WorkflowState workflowState, UserDetails userDetails) {
            UUID newId = workflowTaskId != null ? UUID.fromString(workflowTaskId) : UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".workflow_task " +
                "(id, artifact_id, artifact_type, workflow_id, workflow_state, created, creator, modified, modifier) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                newId, UUID.fromString(artifactId), artifactType.getText(), UUID.fromString(workflowId), workflowState.toString(),
                ts, userDetails.getUid(), ts, userDetails.getUid());
        return newId.toString();
    }

    public void deleteWorkflowTask(String workflowTaskId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".workflow_task "
                + " WHERE id = ?", UUID.fromString(workflowTaskId));
    }

    public List<WorkflowProcessDefinition> getWorkflowProcessDefinitions(UserDetails userDetails) {
        String query = "SELECT * FROM da_" + userDetails.getTenant() + ".workflow_settings";
        return jdbcTemplate.query(query, new WorkflowProcessDefinitionMapper());
    }

    public String getWorkflowStateName(String state, UserDetails userDetails) {
        return jdbcTemplate.queryForObject("SELECT name FROM da_" + userDetails.getTenant() + ".workflow_state WHERE state=?", String.class, state);
    }

    public SearchResponse<FlatWorkflowProcessDefinition> searchSettings(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, UserDetails userDetails) {

        SearchSQLParts searchSQLParts = getSearchSQLParts(searchRequest, searchableColumns, null, false, userDetails);

        String orderby = searchSQLParts.getOrderBy();
        String where = searchSQLParts.getWhere();
        String join = searchSQLParts.getJoin();
        List<Object> whereValues = searchSQLParts.getWhereValues();

        String subQuery = "SELECT s.* FROM da_" + userDetails.getTenant() + ".workflow_settings s ";

        subQuery = "SELECT sq.* FROM (" + subQuery + ") as sq  ";

        List<FlatWorkflowProcessDefinition> flatItems =
                jdbcTemplate.query("SELECT tbl1.*, at.name AS artifact_type_name FROM (" + subQuery + ") as tbl1"
                        + " LEFT JOIN da_" + userDetails.getTenant() + ".artifact_type at ON tbl1.artifact_type=at.code "
                        + where
                        + " ORDER BY " + orderby + " OFFSET " + searchRequest.getOffset() + " LIMIT "
                        + searchRequest.getLimit(), new FlatWorkflowProcessDefinitionRowMapper(), whereValues.toArray());

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(distinct tbl1.id) FROM (" + subQuery + ") as tbl1"
                + " LEFT JOIN da_" + userDetails.getTenant() + ".artifact_type at ON tbl1.artifact_type=at.code "
                + where, Integer.class, whereValues.toArray());

        SearchResponse<FlatWorkflowProcessDefinition> res = new SearchResponse<>(total, searchRequest.getLimit(), searchRequest.getOffset(), flatItems);

        return res;
    }

    public boolean settingsExists(String id, UserDetails userDetails) {

        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(id) FROM da_" + userDetails.getTenant() + ".workflow_settings WHERE id=?",
                Integer.class, UUID.fromString(id));
        return c > 0;
    }

    public void deleteSettingsById(String id, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".workflow_settings WHERE id=?", UUID.fromString(id));
    }

    public WorkflowProcessDefinition getSettingsById(String id, UserDetails userDetails) {
        String query =" SELECT s.* FROM da_" + userDetails.getTenant() + ".workflow_settings s WHERE s.id=? ";
        List<WorkflowProcessDefinition> pds = jdbcTemplate.query(query, new WorkflowProcessDefinitionMapper(), UUID.fromString(id));
        if (pds.isEmpty())
            return null;
        return pds.get(0);
    }

    public WorkflowProcessDefinition updateSettings(String id, UpdatableWorkflowProcessDefinition pd, UserDetails userDetails) throws LottabyteException {

        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (pd.getArtifactType() != null) {
            sets.add("artifact_type=?");
            args.add(pd.getArtifactType());
        }
        if (pd.getArtifactAction() != null) {
            sets.add("artifact_action=?");
            args.add(pd.getArtifactAction());
        }
        if (pd.getProcessDefinitionKey() != null) {
            sets.add("process_definition_key=?");
            args.add(pd.getProcessDefinitionKey());
        }
        if (pd.getDescription() != null) {
            sets.add("description=?");
            args.add(pd.getDescription());
        }

        if (sets.size() > 0) {
            sets.add("modified=?");
            sets.add("modifier=?");
            args.add(LocalDateTime.now());
            args.add(userDetails.getUid());
            args.add(UUID.fromString(id));

            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".workflow_settings SET " + StringUtils.join(sets, ", ")
                    + " WHERE id=?", args.toArray());
        }
        return getSettingsById(id, userDetails);
    }

    public String createSettings(UpdatableWorkflowProcessDefinition pd, UserDetails userDetails) throws LottabyteException {
        UUID newId = UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".workflow_settings (id, artifact_type, artifact_action, process_definition_key, description, created, creator, modified, modifier) VALUES (?,?,?,?,?,?,?,?,?)",
                newId, pd.getArtifactType().getText(), pd.getArtifactAction().name(),
                pd.getProcessDefinitionKey(), pd.getDescription(),
                ts, userDetails.getUid(), ts, userDetails.getUid());
        return newId.toString();
    }
}
