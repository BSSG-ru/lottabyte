package ru.bssg.lottabyte.core.model.entitySample;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update entity sample object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableEntitySampleEntity extends EntitySampleEntity {

    public UpdatableEntitySampleEntity(EntitySampleEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setEntityId(fromCopy.getEntityId());
        this.setSystemId(fromCopy.getSystemId());
        this.setEntityQueryId(fromCopy.getEntityQueryId());
        this.setLastUpdated(fromCopy.getLastUpdated());
        this.setIsMain(fromCopy.getIsMain());
        this.setRoles(fromCopy.getRoles());
    }

}
