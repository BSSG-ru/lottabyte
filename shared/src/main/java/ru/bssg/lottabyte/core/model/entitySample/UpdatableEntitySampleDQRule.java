package ru.bssg.lottabyte.core.model.entitySample;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Sample DQRule Entity")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UpdatableEntitySampleDQRule extends EntitySampleDQRuleEntity {

    public UpdatableEntitySampleDQRule(EntitySampleDQRuleEntity fromCopy) throws LottabyteException {
        this.setArtifactType(fromCopy.getArtifactType());
        this.setSettings(fromCopy.getSettings());
        this.setDqRuleId(fromCopy.getDqRuleId());
        this.setDisabled(fromCopy.getDisabled());
        this.setIndicatorId(fromCopy.getIndicatorId());
        this.setProductId(fromCopy.getProductId());
        this.setSendMail(fromCopy.getSendMail());
        this.setEntitySampleId(fromCopy.getEntitySampleId());
        this.setId(fromCopy.getId());
    }

}
