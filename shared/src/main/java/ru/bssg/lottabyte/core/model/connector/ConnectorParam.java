package ru.bssg.lottabyte.core.model.connector;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class ConnectorParam extends ModeledObject<ConnectorParamEntity> {

    public ConnectorParam() throws LottabyteException {
    }

    public ConnectorParam(ConnectorParamEntity entity) throws LottabyteException {
        super(entity);
    }

    public ConnectorParam(ConnectorParamEntity entity, Metadata md) throws LottabyteException {
        super(entity, md, ArtifactType.connector_param);
    }

}
