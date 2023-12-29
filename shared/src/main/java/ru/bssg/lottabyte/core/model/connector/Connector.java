package ru.bssg.lottabyte.core.model.connector;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.domain.DomainEntity;

public class Connector extends ModeledObject<ConnectorEntity> {

    public Connector() throws LottabyteException {
    }

    public Connector(ConnectorEntity entity) throws LottabyteException {
        super(entity);
    }

    public Connector(ConnectorEntity entity, Metadata md) throws LottabyteException {
        super(entity, md, ArtifactType.connector);
    }

}
