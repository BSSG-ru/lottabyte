package ru.bssg.lottabyte.core.model.backupRun;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper=false)
public class BackupRunEntity extends Entity {
    private String path;
    private String resultMsg;
    private Integer tenantId;
    private LocalDateTime backupStart;
    private LocalDateTime backupEnd;
    private String backupState;
    private LocalDateTime lastUpdated;

    public BackupRunEntity() {
        super(ArtifactType.backup_run);
    }

}
