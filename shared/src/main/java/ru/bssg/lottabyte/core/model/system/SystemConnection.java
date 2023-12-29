package ru.bssg.lottabyte.core.model.system;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.connector.ConnectorEntity;

public class SystemConnection extends ModeledObject<SystemConnectionEntity> {

    public SystemConnection() throws LottabyteException {
    }

    public SystemConnection(SystemConnectionEntity entity) throws LottabyteException {
        super(entity);
    }

    public SystemConnection(SystemConnectionEntity entity, Metadata md) throws LottabyteException {
        super(entity, md, ArtifactType.system_connection);
    }

}
