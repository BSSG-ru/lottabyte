package ru.bssg.lottabyte.core.model.backupRun;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update backup run object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableBackupRunEntity extends BackupRunEntity {

    public UpdatableBackupRunEntity(BackupRunEntity fromCopy) {
        this.setName(fromCopy.getName());
        this.setPath(fromCopy.getPath());
        this.setTenantId(fromCopy.getTenantId());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setLastUpdated(fromCopy.getLastUpdated());
        this.setResultMsg(fromCopy.getResultMsg());
    }

}
