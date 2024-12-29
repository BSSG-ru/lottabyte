package ru.bssg.lottabyte.scheduler.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.task.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@Slf4j
public class TaskScheduleRepository {
    private final JdbcTemplate jdbcTemplate;
    @Autowired
    public TaskScheduleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    static class TaskScheduleRowMapper implements RowMapper<TaskSchedule> {
        @Override
        public TaskSchedule mapRow(ResultSet rs, int rowNum) throws SQLException {
            TaskSchedule ts = null;

            TaskScheduleEntity e = new TaskScheduleEntity();

            e.setDescription(rs.getString("description"));
            e.setEnabled(rs.getBoolean("enabled"));
            e.setScheduleType(TaskSchedulerType.valueOf(rs.getString("schedule_type")));
            e.setScheduleParams(rs.getString("schedule_params"));

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setCreatedBy(rs.getString("creator"));
            md.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
            md.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
            md.setModifiedBy(rs.getString("modifier"));
            md.setArtifactType(e.getArtifactType().toString());
            ts = new TaskSchedule(e, md);

            return ts;
        }
    }

    public List<TaskSchedule> getSchedules(String taskId, UserDetails userDetails) {
        return jdbcTemplate.query("SELECT * FROM da_" + userDetails.getTenant() + ".task_schedule WHERE " +
                "task_id=? ORDER BY created", new TaskScheduleRowMapper(), UUID.fromString(taskId));
    }

    public void updateTaskScheduleEnabled(String taskScheduleId, UserDetails userDetails) {
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da_" + userDetails.getTenant() + ".task_schedule SET enabled=false, modifier = ?, modified = ?";
        params.add(userDetails.getUid());
        params.add(new Timestamp(new java.util.Date().getTime()));

        query += " WHERE id = ?";
        params.add(UUID.fromString(taskScheduleId));
        jdbcTemplate.update(query, params.toArray());
    }
}
