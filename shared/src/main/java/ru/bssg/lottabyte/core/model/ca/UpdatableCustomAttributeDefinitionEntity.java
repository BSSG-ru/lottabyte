package ru.bssg.lottabyte.core.model.ca;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update CustomAttributeDefinition object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableCustomAttributeDefinitionEntity extends CustomAttributeDefinitionEntity {

    public UpdatableCustomAttributeDefinitionEntity(CustomAttributeDefinitionEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setDefaultValue(fromCopy.getDefaultValue());
        this.setMaximum(fromCopy.getMaximum());
        this.setMinimum(fromCopy.getMinimum());
        this.setMinLength(fromCopy.getMinLength());
        this.setMaxLength(fromCopy.getMaxLength());
        this.setMultipleValues(fromCopy.getMultipleValues());
        this.setType(fromCopy.getType());
        this.setPlaceholder(fromCopy.getPlaceholder());
        this.setDefaultValue(fromCopy.getDefaultValue());
    }
}
