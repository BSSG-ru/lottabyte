package ru.bssg.lottabyte.core.model.system;

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
        description = "Update system folder object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableSystemFolderEntity extends SystemFolderEntity {

    public UpdatableSystemFolderEntity(SystemFolderEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setParentId(fromCopy.getParentId());
        if (fromCopy.getChildren() != null) {
            this.setChildren(new ArrayList<>(fromCopy.getChildren()));
        }
    }

}
