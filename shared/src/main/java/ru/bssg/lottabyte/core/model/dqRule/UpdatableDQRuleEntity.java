package ru.bssg.lottabyte.core.model.dqRule;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Update DQRule")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UpdatableDQRuleEntity extends DQRuleEntity {

    public UpdatableDQRuleEntity(DQRuleEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setRuleRef(fromCopy.getRuleRef());
        this.setSettings(fromCopy.getSettings());
        this.setRuleTypeId(fromCopy.getRuleTypeId());
    }

}
