package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.tag.Tag;
import ru.bssg.lottabyte.core.model.taskrun.TaskRun;
import ru.bssg.lottabyte.core.model.taskrun.TaskRunEntity;
import ru.bssg.lottabyte.core.model.taskrun.UpdatableTaskRunEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.*;
import java.util.Date;
import java.util.*;

@Repository
@Slf4j
public class TaskRunRepository extends GenericArtifactRepository<TaskRun> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = {"task_id", "result_sample_id", "result_sample_version_id", "result_msg", "stared_by", "start_mode", "task_start", "task_end", "task_state", "last_updated"};

    @Autowired
    public TaskRunRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.task_run.name(), extFields);
        super.setMapper(new TaskRunRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    class TaskRunRowMapper implements RowMapper<TaskRun> {
        @Override
        public TaskRun mapRow(ResultSet rs, int rowNum) throws SQLException {
            TaskRun taskRun = null;

            TaskRunEntity taskRunEntity = new TaskRunEntity();
            taskRunEntity.setTaskId(rs.getString("task_id"));
            taskRunEntity.setResultSampleId(rs.getString("result_sample_id"));
            taskRunEntity.setResultSampleVersionId(rs.getInt("result_sample_version_id"));
            taskRunEntity.setResultMsg(rs.getString("result_msg"));
            taskRunEntity.setStaredBy(rs.getString("stared_by"));
            taskRunEntity.setStartMode(rs.getString("start_mode"));
            if(rs.getTimestamp("task_start") != null)
                taskRunEntity.setTaskStart(rs.getTimestamp("task_start").toLocalDateTime());
            if(rs.getTimestamp("task_end") != null)
                taskRunEntity.setTaskEnd(rs.getTimestamp("task_end").toLocalDateTime());
            taskRunEntity.setTaskState(rs.getString("task_state"));
            if(rs.getTimestamp("last_updated") != null)
                taskRunEntity.setLastUpdated(rs.getTimestamp("last_updated").toLocalDateTime());

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setArtifactType(taskRunEntity.getArtifactType().toString());

            try {
                taskRun = new TaskRun(taskRunEntity, md);
            } catch (LottabyteException e) {
                log.error(e.getMessage(), e);
            }
            return taskRun;
        }
    }

    public List<TaskRun> getTaskRunListByTaskId(String taskId, UserDetails userDetails) {
        List<TaskRun> taskRunList = jdbcTemplate.query("SELECT id, task_id, result_sample_id, result_sample_version_id, result_msg, stared_by, start_mode, task_start, task_end, task_state, last_updated FROM da_" + userDetails.getTenant() + ".task_run " +
                        "WHERE task_id=?" +
                        "AND task_end is null",
                new TaskRunRowMapper(), UUID.fromString(taskId));

        if (taskRunList.isEmpty())
            return Collections.emptyList();

        return taskRunList;
    }

    public String createTaskRun(UpdatableTaskRunEntity updatableTaskRunEntity, UserDetails userDetails) {
        UUID uuidTaskRun = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".task_run " +
                        "(id, task_id, result_sample_id, result_sample_version_id, result_msg, stared_by, start_mode, task_start, task_state, last_updated) " +
                        "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
                uuidTaskRun, UUID.fromString(updatableTaskRunEntity.getTaskId()), updatableTaskRunEntity.getResultSampleId() != null ? UUID.fromString(updatableTaskRunEntity.getResultSampleId()) : null, updatableTaskRunEntity.getResultSampleVersionId(),
                updatableTaskRunEntity.getResultMsg(), updatableTaskRunEntity.getStaredBy(), updatableTaskRunEntity.getStartMode(), new Timestamp(new Date().getTime()), updatableTaskRunEntity.getTaskState(), new Timestamp(new Date().getTime()));
        return uuidTaskRun.toString();
    }

    public void updateTaskRunById(String taskRunId, UpdatableTaskRunEntity updatableTaskRunEntity, UserDetails userDetails) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".task_run SET last_updated = ?";
        params.add(new Timestamp(new java.util.Date().getTime()));

        if (updatableTaskRunEntity.getTaskId() != null) {
            sets.add("task_id = ?");
            params.add(UUID.fromString(updatableTaskRunEntity.getTaskId()));
        }
        if (updatableTaskRunEntity.getResultSampleId() != null) {
            sets.add("result_sample_id = ?");
            params.add(UUID.fromString(updatableTaskRunEntity.getResultSampleId()));
        }
        if (updatableTaskRunEntity.getResultSampleVersionId() != null) {
            sets.add("result_sample_version_id = ?");
            params.add(updatableTaskRunEntity.getResultSampleVersionId());
        }
        if (updatableTaskRunEntity.getResultMsg() != null) {
            sets.add("result_msg = ?");
            params.add(updatableTaskRunEntity.getResultMsg());
        }
        if (updatableTaskRunEntity.getStaredBy() != null) {
            sets.add("stared_by = ?");
            params.add(updatableTaskRunEntity.getStaredBy());
        }
        if (updatableTaskRunEntity.getStartMode() != null) {
            sets.add("start_mode = ?");
            params.add(updatableTaskRunEntity.getStartMode());
        }
        if (updatableTaskRunEntity.getTaskStart() != null) {
            sets.add("task_start = ?");
            params.add(updatableTaskRunEntity.getTaskStart());
        }
        if (updatableTaskRunEntity.getTaskEnd() != null) {
            sets.add("task_end = ?");
            params.add(updatableTaskRunEntity.getTaskEnd());
        }
        if (updatableTaskRunEntity.getTaskState() != null) {
            sets.add("task_state = ?");
            params.add(updatableTaskRunEntity.getTaskState());
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(taskRunId));
            jdbcTemplate.update(query, params.toArray());
        }
    }
}
