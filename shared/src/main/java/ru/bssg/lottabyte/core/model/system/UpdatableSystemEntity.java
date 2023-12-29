package ru.bssg.lottabyte.core.model.system;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update system object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableSystemEntity extends SystemEntity {

    public UpdatableSystemEntity(SystemEntity fromCopy) {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setConnectorId(fromCopy.getConnectorId());
        this.setSystemFolderId(fromCopy.getSystemFolderId());
        this.setSystemType(fromCopy.getSystemType());
        this.setDomainIds(fromCopy.getDomainIds());
    }

}
