package ru.bssg.lottabyte.core.model.dataentity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.system.SystemFolderEntity;

import java.util.ArrayList;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update entity object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableDataEntityEntity extends DataEntityEntity {

    public UpdatableDataEntityEntity(DataEntityEntity fromCopy) {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setEntityFolderId(fromCopy.getEntityFolderId());
        this.setBusinessEntityId(fromCopy.getBusinessEntityId());
        this.setRoles(fromCopy.getRoles());
        if (fromCopy.getSystemIds() != null) {
            this.setSystemIds(new ArrayList<>(fromCopy.getSystemIds()));
        }
    }

}
