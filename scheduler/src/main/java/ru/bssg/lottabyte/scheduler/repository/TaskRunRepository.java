package ru.bssg.lottabyte.scheduler.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.taskrun.TaskRun;
import ru.bssg.lottabyte.core.model.taskrun.TaskRunEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Repository
@Slf4j
public class TaskRunRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TaskRunRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    static class TaskRunRowMapper implements RowMapper<TaskRun> {
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

    public TaskRun getTaskRunByTaskId(String taskId, UserDetails userDetails) {
        List<TaskRun> taskRunList = jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".task_run " +
                        "WHERE task_id=?;",
                new TaskRunRowMapper(), UUID.fromString(taskId));

        return taskRunList.stream().findFirst().orElse(null);
    }
    public List<TaskRun> getTaskRunListByTaskId(String taskId, UserDetails userDetails) {
        List<TaskRun> taskRunList = jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".task_run " +
                        "WHERE task_id=?" +
                        "AND task_end is null",
                new TaskRunRowMapper(), UUID.fromString(taskId));

        if (taskRunList.isEmpty())
            return Collections.emptyList();

        return taskRunList;
    }
}
