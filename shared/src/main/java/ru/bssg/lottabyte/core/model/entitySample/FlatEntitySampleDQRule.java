package ru.bssg.lottabyte.core.model.entitySample;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;

import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper=false)
public class FlatEntitySampleDQRule extends FlatModeledObject {

    private String entity_sample_id;
    private String dq_rule_id;
    private String settings;
    private boolean disabled;
    private String indicator_id;
    private String product_id;
    private boolean send_mail;

    public FlatEntitySampleDQRule(EntitySampleDQRule entitySampleDQRule) {
        super(entitySampleDQRule.getFlatModeledObject());
        this.settings = entitySampleDQRule.getEntity().getSettings();
        this.dq_rule_id = entitySampleDQRule.getEntity().getDqRuleId();
        this.entity_sample_id = entitySampleDQRule.getEntity().getEntitySampleId();
        this.disabled = entitySampleDQRule.getEntity().getDisabled();
        this.indicator_id = entitySampleDQRule.getEntity().getIndicatorId();
        this.product_id = entitySampleDQRule.getEntity().getProductId();
        this.send_mail = entitySampleDQRule.getEntity().getSendMail();
    }
}
