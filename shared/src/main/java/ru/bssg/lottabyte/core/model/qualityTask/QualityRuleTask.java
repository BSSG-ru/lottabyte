package ru.bssg.lottabyte.core.model.qualityTask;

import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class QualityRuleTask extends ModeledObject<QualityRuleTaskEntity> {

    public QualityRuleTask() {
    }

    public QualityRuleTask(QualityRuleTaskEntity entity) {
        super(entity);
    }

    public QualityRuleTask(QualityRuleTaskEntity entity, Metadata md) {
        super(entity, md, ArtifactType.qualityRuleTask);
    }
}
