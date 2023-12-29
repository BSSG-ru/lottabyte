package ru.bssg.lottabyte.core.model.reference;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update relation"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UpdatableReferenceEntity extends ReferenceEntity {

    public UpdatableReferenceEntity(ReferenceEntity fromCopy) throws LottabyteException {
        this.setArtifactType(fromCopy.getArtifactType());
        this.setSourceId(fromCopy.getSourceId());
        this.setSourceType(fromCopy.getSourceType());
        this.setTargetId(fromCopy.getTargetId());
        this.setTargetType(fromCopy.getTargetType());
        this.setReferenceType(fromCopy.getReferenceType());
        this.setPublishedId(fromCopy.getPublishedId());
        this.setVersionId(fromCopy.getVersionId());
    }

}
