package ru.bssg.lottabyte.core.model.qualityTask;

import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;


public class QualityTask extends ModeledObject<QualityTaskEntity> {

    public QualityTask() {
    }

    public QualityTask(QualityTaskEntity entity) {
        super(entity);
    }

    public QualityTask(QualityTaskEntity entity, Metadata md) {
        super(entity, md, ArtifactType.task);
    }
}
