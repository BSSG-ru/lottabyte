package ru.bssg.lottabyte.core.model.datatype;

import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class DataType extends ModeledObject<DataTypeEntity> {
    public DataType() {
    }

    public DataType(DataTypeEntity entity) {
        super(entity);
    }

    public DataType(DataTypeEntity entity, Metadata md) {
        super(entity, md, ArtifactType.datatype);
    }
}
