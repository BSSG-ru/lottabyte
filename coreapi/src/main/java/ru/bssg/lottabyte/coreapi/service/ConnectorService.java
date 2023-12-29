package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.connector.Connector;
import ru.bssg.lottabyte.core.model.connector.ConnectorParam;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.ConnectorRepository;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectorService {
    private final ConnectorRepository connectorRepository;

    public Connector getConnectorById(String connectorId, UserDetails userDetails) throws LottabyteException {
        if (connectorId == null || connectorRepository.getById(connectorId, null) == null)
            throw new LottabyteException(Message.LBE01101, userDetails.getLanguage(), connectorId);
        return connectorRepository.getById(connectorId, null);
    }

    public PaginatedArtifactList<Connector> getConnectorsPaginated(Integer offset, Integer limit) {
        return connectorRepository.getAllPaginated(offset, limit, "/v1/connectors/", null);
    }

    public ConnectorParam getConnectorParamById(String connectorParamId) {
        return connectorRepository.getConnectorParamById(connectorParamId);
    }

    public PaginatedArtifactList<ConnectorParam> getConnectorParamsPaginated(String connectorId, Integer offset, Integer limit, UserDetails userDetails) throws LottabyteException {
        if (connectorId == null || connectorRepository.getById(connectorId, null) == null)
            throw new LottabyteException(Message.LBE01101, userDetails.getLanguage(), connectorId);
        return connectorRepository.getConnectorParamsPaginated(connectorId, offset, limit);
    }

    public List<ConnectorParam> getConnectorParamsList(String connectorId, UserDetails userDetails) throws LottabyteException {
        if (connectorId == null || connectorRepository.getById(connectorId, null) == null)
            throw new LottabyteException(Message.LBE01101, userDetails.getLanguage(), connectorId);
        return connectorRepository.getConnectorParamsList(connectorId);
    }
}
