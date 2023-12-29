package ru.bssg.lottabyte.core.model.qualityTask;

import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;


public class QualityTaskAssertion extends ModeledObject<QualityTaskAssertionEntity> {

    public QualityTaskAssertion() {
    }

    public QualityTaskAssertion(QualityTaskAssertionEntity entity) {
        super(entity);
    }

    public QualityTaskAssertion(QualityTaskAssertionEntity entity, Metadata md) {
        super(entity, md, ArtifactType.qualityAssertionTask);
    }
}
