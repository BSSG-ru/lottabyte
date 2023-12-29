package ru.bssg.lottabyte.core.model.dqRule;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.Entity;
import ru.bssg.lottabyte.core.model.ArtifactType;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DQRuleEntity extends Entity {

    private String ruleRef;
    private String settings;
    private String ruleTypeId;

    public DQRuleEntity() {
        super(ArtifactType.dq_rule);
    }

}
