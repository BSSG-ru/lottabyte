package ru.bssg.lottabyte.core.model.system;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class SystemConnectionParam extends ModeledObject<SystemConnectionParamEntity> {

    public SystemConnectionParam() throws LottabyteException {
    }

    public SystemConnectionParam(SystemConnectionParamEntity entity) throws LottabyteException {
        super(entity);
    }

    public SystemConnectionParam(SystemConnectionParamEntity entity, Metadata md) throws LottabyteException {
        super(entity, md, ArtifactType.system_connection_param);
    }

}
