package ru.bssg.lottabyte.core.model.qualityTask;

import java.sql.Timestamp;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

@Data
@EqualsAndHashCode(callSuper=false)
public class QualityRuleTaskEntity extends Entity {

    private String systemId;
    private String systemName;
    private String ruleName;
    private String ruleId;
    private String ruleRef;
    private String ruleSettings;
    private String queryName;
    private String entityName;
    private String dataAssetId;
    private String entitySampleToDqRuleId;
    private String indicatorName;
    private String productName;
    private String dataAssetName;
    private String productId;
    private String indicatorId;
    private String entitySampleId;
    private String entitySampleName;
    private String isCrontab;
    private String status;
    private String ruleTypeId;
    private String ruleTypeName;

    public QualityRuleTaskEntity() {
        super(ArtifactType.qualityRuleTask);
    }

}
