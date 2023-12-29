package ru.bssg.lottabyte.coreapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.backupRun.BackupRun;
import ru.bssg.lottabyte.core.model.backupRun.BackupRunEntity;
import ru.bssg.lottabyte.core.model.backupRun.UpdatableBackupRunEntity;
import ru.bssg.lottabyte.coreapi.util.JDBCUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Repository
@Slf4j
public class BackupRunRepository extends GenericArtifactRepository<BackupRun> {
    private final JdbcTemplate jdbcTemplate;
    private static String[] extFields = {  };

    public BackupRunRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, ArtifactType.backup_run.name(), extFields);
        super.setMapper(new BackupRunRowMapper());
        this.jdbcTemplate = jdbcTemplate;
    }

    class BackupRunRowMapper implements RowMapper<BackupRun> {
        @Override
        public BackupRun mapRow(ResultSet rs, int rowNum) throws SQLException {
            BackupRun backupRun = null;

            BackupRunEntity backupRunEntity = new BackupRunEntity();
            backupRunEntity.setPath(rs.getString("path"));
            backupRunEntity.setResultMsg(rs.getString("result_msg"));
            backupRunEntity.setTenantId(JDBCUtil.getInt(rs, "tenant_id"));
            if(rs.getTimestamp("backup_start") != null)
                backupRunEntity.setBackupStart(rs.getTimestamp("backup_start").toLocalDateTime());
            if(rs.getTimestamp("backup_end") != null)
                backupRunEntity.setBackupEnd(rs.getTimestamp("backup_end").toLocalDateTime());
            backupRunEntity.setBackupState(rs.getString("backup_state"));
            if(rs.getTimestamp("last_updated") != null)
                backupRunEntity.setLastUpdated(rs.getTimestamp("last_updated").toLocalDateTime());

            Metadata md = new Metadata();
            md.setId(rs.getString("id"));
            md.setArtifactType(backupRunEntity.getArtifactType().toString());

            try {
                backupRun = new BackupRun(backupRunEntity, md);
            } catch (LottabyteException e) {
                log.error(e.getMessage(), e);
            }
            return backupRun;
        }
    }

    public String createBackupRun(UpdatableBackupRunEntity updatableBackupRunEntity) {
        UUID uuidBackupRun = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO da.backup_run " +
                        "(id, \"path\", tenant_id, result_msg, backup_start, backup_state, last_updated) " +
                        "VALUES(?, ?, ?, ?, ?, ?, ?);",
                uuidBackupRun, updatableBackupRunEntity.getPath(), updatableBackupRunEntity.getTenantId(),
                updatableBackupRunEntity.getResultMsg(), new Timestamp(new Date().getTime()), updatableBackupRunEntity.getBackupState(), new Timestamp(new Date().getTime()));
        return uuidBackupRun.toString();
    }

    public void updateBackupRunById(String backupRunId, UpdatableBackupRunEntity updatableBackupRunEntity) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        String query = "UPDATE da.backup_run SET last_updated = ?";
        params.add(new Timestamp(new Date().getTime()));

        if (updatableBackupRunEntity.getPath() != null) {
            sets.add("path = ?");
            params.add(updatableBackupRunEntity.getPath());
        }
        if (updatableBackupRunEntity.getResultMsg() != null) {
            sets.add("result_msg = ?");
            params.add(updatableBackupRunEntity.getResultMsg());
        }
        if (updatableBackupRunEntity.getTenantId() != null) {
            sets.add("tenant_id = ?");
            params.add(updatableBackupRunEntity.getTenantId());
        }
        if (updatableBackupRunEntity.getBackupStart() != null) {
            sets.add("backup_start = ?");
            params.add(updatableBackupRunEntity.getBackupStart());
        }
        if (updatableBackupRunEntity.getBackupEnd() != null) {
            sets.add("backup_end = ?");
            params.add(updatableBackupRunEntity.getBackupEnd());
        }
        if (updatableBackupRunEntity.getBackupState() != null) {
            sets.add("backup_state = ?");
            params.add(updatableBackupRunEntity.getBackupState());
        }
        if (!sets.isEmpty()) {
            query += ", " + String.join(",", sets);
            query += " WHERE id = ?";
            params.add(UUID.fromString(backupRunId));
            jdbcTemplate.update(query, params.toArray());
        }
    }
}
