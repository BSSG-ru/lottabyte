package ru.bssg.lottabyte.core.model.etl;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.api.LottabyteException;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Update etl")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UpdatableETLEntity extends ETLEntity {
    public UpdatableETLEntity(ETLEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setCode(fromCopy.getCode());
        this.setAlgorithm(fromCopy.getAlgorithm());
        this.setSystemId(fromCopy.getSystemId());
        this.setEtlTypeId(fromCopy.getEtlTypeId());
        this.setBusinessEntityIds(fromCopy.getBusinessEntityIds());
        this.setSourceIds(fromCopy.getSourceIds());
        this.setTargetIds(fromCopy.getTargetIds());
    }
}
