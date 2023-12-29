package ru.bssg.lottabyte.coreapi.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.workflow.*;
import ru.bssg.lottabyte.core.model.workflowTaskAction.UpdatableWorkflowTaskActionEntity;
import ru.bssg.lottabyte.core.model.workflowTaskAction.WorkflowTaskAction;
import ru.bssg.lottabyte.core.model.workflowTaskAction.WorkflowTaskActionEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Repository
@Slf4j
@RequiredArgsConstructor
public class WorkflowTaskActionRepository {

    private final JdbcTemplate jdbcTemplate;

    class WorkflowActionParamResultRowMapper implements RowMapper<WorkflowActionParamResult> {
        @Override
        public WorkflowActionParamResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            WorkflowActionParamResult workflowActionParamResult = new WorkflowActionParamResult();
            workflowActionParamResult.setId(rs.getString("param_id"));
            workflowActionParamResult.setParamName(rs.getString("param_name"));
            workflowActionParamResult.setParamType(rs.getString("param_type"));
            workflowActionParamResult.setParamValue(rs.getString("param_value"));

            return workflowActionParamResult;
        }
    }
    class WorkflowTaskActionRowMapper implements RowMapper<WorkflowTaskAction> {
        @Override
        public WorkflowTaskAction mapRow(ResultSet rs, int rowNum) throws SQLException {
            WorkflowTaskAction workflowTaskAction = null;

            WorkflowTaskActionEntity workflowTaskActionEntity = new WorkflowTaskActionEntity();
            workflowTaskActionEntity.setWorkflowTaskId(rs.getString("workflow_task_id"));
            workflowTaskActionEntity.setWorkflowAction(rs.getString("workflow_action_id"));

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setArtifactType(workflowTaskActionEntity.getArtifactType().toString());

            try {
                workflowTaskAction = new WorkflowTaskAction(workflowTaskActionEntity, md);
            } catch (LottabyteException e) {
                log.error(e.getMessage(), e);
            }
            return workflowTaskAction;
        }
    }

    public List<WorkflowTaskAction> getWorkflowTaskActionListByWorkflowId(String workflowId, UserDetails userDetails) {
        List<WorkflowTaskAction> workflowTaskActionList = jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".workflow_task_action " +
                        "WHERE workflow_task_id=?",
                new WorkflowTaskActionRowMapper(), UUID.fromString(workflowId));

        if (workflowTaskActionList.isEmpty())
            return Collections.emptyList();

        return workflowTaskActionList;
    }

    public WorkflowTaskAction getWorkflowTaskActionById(String workflowTaskId, UserDetails userDetails) {
        List<WorkflowTaskAction> workflowTaskActionList = jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".workflow_task_action WHERE id=?",
                new WorkflowTaskActionRowMapper(), UUID.fromString(workflowTaskId));

        if (workflowTaskActionList.isEmpty())
            return null;

        return workflowTaskActionList.get(0);
    }

    public String createWorkflowTaskAction(UpdatableWorkflowTaskActionEntity updatableWorkflowTaskActionEntity, UserDetails userDetails) {
        UUID newId = UUID.randomUUID();
        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".workflow_task_action " +
                "(id, workflow_task_id, created, creator, workflow_action_id) " +
                "values (?, ?, ?, ?, ?)",
                newId, UUID.fromString(updatableWorkflowTaskActionEntity.getWorkflowTaskId()),
                ts, userDetails.getUid(), updatableWorkflowTaskActionEntity.getWorkflowAction());
        return newId.toString();
    }
    public void updateWorkflowTaskActionById(String workflowTaskId, UpdatableWorkflowTaskActionEntity updatableWorkflowTaskActionEntity, UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".workflow_task_action SET ";
        params.add(new Timestamp(new Date().getTime()));

        if (updatableWorkflowTaskActionEntity.getWorkflowTaskId() != null) {
            sets.add("workflow_task_id = ?");
            params.add(UUID.fromString(updatableWorkflowTaskActionEntity.getWorkflowTaskId()));
        }
        if (updatableWorkflowTaskActionEntity.getWorkflowAction() != null) {
            sets.add("workflow_action_id = ?");
            params.add(updatableWorkflowTaskActionEntity.getWorkflowAction());
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(workflowTaskId));
            jdbcTemplate.update(query, params.toArray());
        }
    }

    //workflow param

    public List<WorkflowActionParamResult> getWorkflowTaskActionParamListByWorkflowId(String workflowTaskActionId, UserDetails userDetails) {
        List<WorkflowActionParamResult> workflowActionParamResultList = jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".workflow_task_action_param " +
                        "WHERE workflow_task_action_id=?",
                new WorkflowActionParamResultRowMapper(), UUID.fromString(workflowTaskActionId));

        if (workflowActionParamResultList.isEmpty())
            return Collections.emptyList();

        return workflowActionParamResultList;
    }

    public String createWorkflowTaskActionParam(String workflowTaskActionId, WorkflowActionParamResult actionParam, UserDetails userDetails) {
        UUID newId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".workflow_task_action_param " +
                        "(id, workflow_task_action_id, param_id, param_name, param_type, param_value) " +
                        "values (?, ?, ?, ?, ?, ?)",
                newId, UUID.fromString(workflowTaskActionId),
                actionParam.getId() != null ? UUID.fromString(actionParam.getId()) : null, actionParam.getParamName(), actionParam.getParamType(), actionParam.getParamValue());
        return newId.toString();
    }
}
