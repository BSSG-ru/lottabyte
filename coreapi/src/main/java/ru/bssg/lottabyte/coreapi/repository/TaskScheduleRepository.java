package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.task.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@Slf4j
public class TaskScheduleRepository extends GenericArtifactRepository<TaskSchedule> {
    private final JdbcTemplate jdbcTemplate;

    public TaskScheduleRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.task_schedule.name(), new String[] {});
        setMapper(new TaskScheduleRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    class TaskScheduleRowMapper implements RowMapper<TaskSchedule> {

        @Override
        public TaskSchedule mapRow(ResultSet rs, int rowNum) throws SQLException {
            TaskSchedule ts = null;

            TaskScheduleEntity e = new TaskScheduleEntity();
            e.setDescription(rs.getString("description"));
            e.setEnabled(rs.getBoolean("enabled"));
            e.setScheduleType(TaskSchedulerType.valueOf(rs.getString("schedule_type")));
            e.setScheduleParams(rs.getString("schedule_params"));
            e.setTaskId(rs.getString("task_id"));


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
                "task_id=? ORDER BY created", this.mapper, UUID.fromString(taskId));
    }

    public String createTaskSchedule(UpdatableTaskScheduleEntity newTaskScheduleEntity, UserDetails userDetails) {
        UUID id = UUID.randomUUID();

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update("INSERT INTO da_" + userDetails.getTenant() + ".task_schedule (id, task_id, description, enabled, schedule_type, schedule_params, created, creator, modified, modifier) VALUES (?,?,?,?,?,?,?,?,?,?)",
                id, UUID.fromString(newTaskScheduleEntity.getTaskId()), newTaskScheduleEntity.getDescription(), newTaskScheduleEntity.getEnabled(),
                newTaskScheduleEntity.getScheduleType().name(), newTaskScheduleEntity.getScheduleParams(),
                now, userDetails.getUid(), now, userDetails.getUid());

        return id.toString();
    }

    public void updateTaskSchedule(String taskScheduleId, UpdatableTaskScheduleEntity taskScheduleEntity, UserDetails userDetails) {
        LocalDateTime now = LocalDateTime.now();

        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (taskScheduleEntity.getEnabled() != null) {
            sets.add("enabled=?");
            args.add(taskScheduleEntity.getEnabled());
        }
        if (taskScheduleEntity.getScheduleParams() != null) {
            sets.add("schedule_params=?");
            args.add(taskScheduleEntity.getScheduleParams());
        }
        if (taskScheduleEntity.getScheduleType() != null) {
            sets.add("schedule_type=?");
            args.add(taskScheduleEntity.getScheduleType().name());
        }
        if (taskScheduleEntity.getDescription() != null) {
            sets.add("description=?");
            args.add(taskScheduleEntity.getDescription());
        }
        if (sets.size() > 0) {
            sets.add("modified=?");
            sets.add("modifier=?");
            args.add(now);
            args.add(userDetails.getUid());
            args.add(UUID.fromString(taskScheduleId));

            jdbcTemplate.update("UPDATE da_" + userDetails.getTenant() + ".task_schedule SET " + StringUtils.join(sets, ", ") + " WHERE id=?", args.toArray());
        }
    }

    public void updateTaskSchedules(Task task, List<TaskSchedule> schedules, UserDetails userDetails) throws LottabyteException {
        for (TaskSchedule ts : task.getEntity().getSchedules()) {
            if (!schedules.stream().anyMatch(x -> x.getId().equals(ts.getId())))
                deleteById(ts.getId(), userDetails);
        }

        for (TaskSchedule ts : schedules) {
            if (task.getEntity().getSchedules().stream().anyMatch(x -> x.getId().equals(ts.getId())))
                updateTaskSchedule(ts.getId(), new UpdatableTaskScheduleEntity(ts.getEntity()), userDetails);
            else
                createTaskSchedule(new UpdatableTaskScheduleEntity(ts.getEntity()), userDetails);
        }
    }

    public void createTaskSchedules(String taskId, List<TaskSchedule> schedules, UserDetails userDetails) throws LottabyteException {
        for (TaskSchedule ts : schedules) {
            ts.getEntity().setTaskId(taskId);
            createTaskSchedule(new UpdatableTaskScheduleEntity(ts.getEntity()), userDetails);
        }
    }

    public void deleteTaskSchedules(String taskId, UserDetails userDetails) {
        jdbcTemplate.update("DELETE FROM da_" + userDetails.getTenant() + ".task_schedule WHERE task_id=?",
                UUID.fromString(taskId));
    }
}
