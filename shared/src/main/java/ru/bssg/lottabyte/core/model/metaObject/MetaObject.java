package ru.bssg.lottabyte.core.model.metaObject;

import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class MetaObject extends ModeledObject<MetaObjectEntity> {
    public MetaObject() {

    }

    public MetaObject(MetaObjectEntity entity) {
        super(entity);
    }

    public MetaObject(MetaObjectEntity entity, Metadata metadata) {
        super(entity, metadata, ArtifactType.meta_object);
    }
}
