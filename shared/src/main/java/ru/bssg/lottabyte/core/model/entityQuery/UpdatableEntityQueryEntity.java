package ru.bssg.lottabyte.core.model.entityQuery;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.dataentity.DataEntityFolderEntity;

import java.util.ArrayList;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update entity query object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableEntityQueryEntity extends EntityQueryEntity {

    public UpdatableEntityQueryEntity(EntityQueryEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setQueryText(fromCopy.getQueryText());
        this.setEntityId(fromCopy.getEntityId());
        this.setSystemId(fromCopy.getSystemId());
    }

}
