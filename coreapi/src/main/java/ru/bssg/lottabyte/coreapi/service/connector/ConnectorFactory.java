package ru.bssg.lottabyte.coreapi.service.connector;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.connector.ConnectorType;
import ru.bssg.lottabyte.core.connector.IConnectorService;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

public class ConnectorFactory {
    public IConnectorService getConnector(ConnectorType type, UserDetails userDetails) throws LottabyteException {
        IConnectorService toReturn = null;
        switch (type) {
            case JDBC:
                toReturn = new GenericJDBCConnectorServiceImpl();
                break;
            case S3:
                toReturn = new ExcelS3ConnectorServiceImpl();
                break;
            case REST_API:
                toReturn = new GenericRESTAPIConnectorServiceImpl();
                break;
            default:
                throw new LottabyteException(Message.LBE00017, userDetails.getLanguage(), type);
        }
        return toReturn;
    }
}
