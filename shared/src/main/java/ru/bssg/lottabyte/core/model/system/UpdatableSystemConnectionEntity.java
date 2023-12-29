package ru.bssg.lottabyte.core.model.system;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update system connection object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableSystemConnectionEntity extends SystemConnectionEntity {
    protected List<SystemConnectionParamEntity> connectorParam;

    public UpdatableSystemConnectionEntity(SystemConnectionEntity fromCopy, List<SystemConnectionParamEntity> systemConnectionParamEntityList) {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setConnectorId(fromCopy.getConnectorId());
        this.setSystemId(fromCopy.getSystemId());
        this.setEnabled(fromCopy.getEnabled());
        this.setConnectorParam(systemConnectionParamEntityList);
    }
}
