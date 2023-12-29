package ru.bssg.lottabyte.core.connector;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.connector.Connector;
import ru.bssg.lottabyte.core.model.connector.ConnectorParam;
import ru.bssg.lottabyte.core.model.dataentity.DataEntity;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQueryResult;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.model.system.SystemConnection;
import ru.bssg.lottabyte.core.model.system.SystemConnectionParam;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

import java.sql.SQLException;
import java.util.List;

public interface IConnectorService {

    public EntityQueryResult querySystem(Connector connector,
                                         List<ConnectorParam> connectorParams,
                                         System system,
                                         DataEntity entity,
                                         EntityQuery entityQuery,
                                         SystemConnection systemConnection,
                                         List<SystemConnectionParam> systemConnectionParams,
                                         UserDetails userDetails)
            throws LottabyteException, SQLException, ClassNotFoundException;

}
