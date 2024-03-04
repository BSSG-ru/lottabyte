package ru.bssg.lottabyte.core.model.qualityTask;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper=false)
public class FlatQualityRuleTask extends FlatModeledObject {

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

    public FlatQualityRuleTask(QualityRuleTask d) {
        super(d.getFlatModeledObject());
        this.systemId = d.getEntity().getSystemId();
        this.systemName = d.getEntity().getSystemName();
        this.ruleName = d.getEntity().getRuleName();
        this.ruleId = d.getEntity().getRuleId();
        this.ruleRef = d.getEntity().getRuleRef();
        this.ruleSettings = d.getEntity().getRuleSettings();
        this.queryName = d.getEntity().getQueryName();
        this.entityName = d.getEntity().getEntityName();
        this.dataAssetId = d.getEntity().getDataAssetId();
        this.entitySampleToDqRuleId = d.getEntity().getEntitySampleToDqRuleId();
        this.indicatorName = d.getEntity().getIndicatorName();
        this.productName = d.getEntity().getProductName();
        this.dataAssetName = d.getEntity().getDataAssetName();
        this.productId = d.getEntity().getProductId();
        this.indicatorId = d.getEntity().getIndicatorId();
        this.entitySampleId = d.getEntity().getEntitySampleId();
        this.entitySampleName = d.getEntity().getEntitySampleName();
        this.isCrontab = d.getEntity().getIsCrontab();
        this.status = d.getEntity().getStatus();
        this.ruleTypeId = d.getEntity().getRuleTypeId();
        this.ruleTypeName = d.getEntity().getRuleTypeName();
    }
}
