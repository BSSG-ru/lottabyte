package ru.bssg.lottabyte.core.model.systemType;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class SystemType extends ModeledObject<SystemTypeEntity> {

    public SystemType() throws LottabyteException {
    }

    public SystemType(SystemTypeEntity entity) throws LottabyteException {
        super(entity);
    }

    public SystemType(SystemTypeEntity entity, Metadata md) throws LottabyteException {
        super(entity, md, ArtifactType.system_type);
    }

}
