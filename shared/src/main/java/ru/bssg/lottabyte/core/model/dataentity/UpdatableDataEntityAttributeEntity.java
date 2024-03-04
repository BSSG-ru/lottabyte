package ru.bssg.lottabyte.core.model.dataentity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;

import java.util.ArrayList;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update entity attribute object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableDataEntityAttributeEntity extends DataEntityAttributeEntity {

    public UpdatableDataEntityAttributeEntity(DataEntityAttributeEntity fromCopy) {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setAttributeType(fromCopy.getAttributeType());
        this.setEntityId(fromCopy.getEntityId());
        this.setEnumerationId(fromCopy.getEnumerationId());
        this.setAttributeId(fromCopy.getAttributeId());
        this.setIsPk(fromCopy.getIsPk());
        if (fromCopy.getMappedSamplePropertyIds() != null) {
            this.setMappedSamplePropertyIds(new ArrayList<>(fromCopy.getMappedSamplePropertyIds()));
        }
    }
}
