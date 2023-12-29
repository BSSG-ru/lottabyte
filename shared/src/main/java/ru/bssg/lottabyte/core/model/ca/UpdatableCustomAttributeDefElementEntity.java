package ru.bssg.lottabyte.core.model.ca;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;

import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update CustomAttributeDefElement object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableCustomAttributeDefElementEntity extends CustomAttributeDefElementEntity {

    public UpdatableCustomAttributeDefElementEntity(CustomAttributeDefElementEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setDefinitionId(fromCopy.getDefinitionId());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
    }

}
