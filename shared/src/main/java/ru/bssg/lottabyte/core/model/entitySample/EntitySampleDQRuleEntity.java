package ru.bssg.lottabyte.core.model.entitySample;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class EntitySampleDQRuleEntity extends Entity {

    private String entitySampleId;
    private String dqRuleId;
    private String settings;
    private Boolean disabled;
    private String indicatorId;
    private String productId;
    private String assetId;
    private Boolean sendMail;
    private Integer historyId;
    private String publishedId;
    private String ancestorId;

    public EntitySampleDQRuleEntity() {
        super(ArtifactType.entity_sample_to_dq_rule);
    }

}
