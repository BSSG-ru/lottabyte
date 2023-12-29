package ru.bssg.lottabyte.scheduler.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.task.Task;
import ru.bssg.lottabyte.core.model.task.TaskEntity;
import ru.bssg.lottabyte.core.model.task.TaskSchedulerType;
import ru.bssg.lottabyte.core.model.task.UpdatableTaskEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@Slf4j
public class TaskRepository {
    private final JdbcTemplate jdbcTemplate;
    @Autowired
    public TaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    static class TaskRowMapper implements RowMapper<Task> {
        @Override
        public Task mapRow(ResultSet rs, int rowNum) throws SQLException {
            Task task = null;

            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setName(rs.getString("name"));
            taskEntity.setDescription(rs.getString("description"));
            taskEntity.setEnabled(rs.getBoolean("enabled"));
            taskEntity.setScheduleType(TaskSchedulerType.valueOf(rs.getString("schedule_type")));
            taskEntity.setQueryId(rs.getString("query_id"));
            taskEntity.setSystemConnectionId(rs.getString("system_connection_id"));
            taskEntity.setScheduleParams(rs.getString("schedule_params"));

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));
            md.setName(rs.getString("name"));
            md.setArtifactType(taskEntity.getArtifactType().toString());
            task = new Task(taskEntity, md);

            return task;
        }
    }

    public List<Task> getAllTasks(UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".task",
                new TaskRowMapper());
    }

    public void updateTaskEnabled(String taskId, UserDetails userDetails) {
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".task SET enabled=false, modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));

        query += " WHERE id = ?";
        params.add(UUID.fromString(taskId));
        jdbcTemplate.update(query, params.toArray());
    }
}
