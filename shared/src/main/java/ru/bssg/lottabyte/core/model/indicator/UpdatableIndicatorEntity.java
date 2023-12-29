package ru.bssg.lottabyte.core.model.indicator;

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
        description = "Update indicator"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UpdatableIndicatorEntity extends IndicatorEntity {

    public UpdatableIndicatorEntity(IndicatorEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setCalcCode(fromCopy.getCalcCode());
        this.setFormula(fromCopy.getFormula());
        if (fromCopy.getDqChecks() != null) {
            this.setDqChecks(new ArrayList<>(fromCopy.getDqChecks()));
        }
        /*if (fromCopy.getEntityAttributes() != null) {
            this.setEntityAttributes(new ArrayList<>(fromCopy.getEntityAttributes()));
        }*/
        this.setDomainId(fromCopy.getDomainId());
        this.setIndicatorTypeId(fromCopy.getIndicatorTypeId());
        if (fromCopy.getDataAssetIds() != null)
            this.setDataAssetIds(new ArrayList<>(fromCopy.getDataAssetIds()));
        this.setExamples(fromCopy.getExamples());
        this.setLink(fromCopy.getLink());
        this.setDatatypeId(fromCopy.getDatatypeId());
        this.setLimits(fromCopy.getLimits());
        this.setLimits_internal(fromCopy.getLimits_internal());
        this.setRoles(fromCopy.getRoles());
    }

}
