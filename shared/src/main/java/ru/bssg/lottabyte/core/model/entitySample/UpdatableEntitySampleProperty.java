package ru.bssg.lottabyte.core.model.entitySample;

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
        description = "Update entity sample property object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableEntitySampleProperty extends EntitySamplePropertyEntity {

    public UpdatableEntitySampleProperty(EntitySamplePropertyEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setPathType(fromCopy.getPathType());
        this.setPath(fromCopy.getPath());
        this.setType(fromCopy.getType());
        this.setEntitySampleId(fromCopy.getEntitySampleId());
        if (fromCopy.getMappedAttributeIds() != null) {
            this.setMappedAttributeIds(new ArrayList<>(fromCopy.getMappedAttributeIds()));
        }
    }

}
