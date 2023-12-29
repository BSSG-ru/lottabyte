package ru.bssg.lottabyte.core.model.qualityTask;

import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class QualityTaskRun extends ModeledObject<QualityTaskRunEntity> {

    public QualityTaskRun() {
    }

    public QualityTaskRun(QualityTaskRunEntity entity) {
        super(entity);
    }

    public QualityTaskRun(QualityTaskRunEntity entity, Metadata md) {
        super(entity, md, ArtifactType.qualityRuleTask);
    }
}
