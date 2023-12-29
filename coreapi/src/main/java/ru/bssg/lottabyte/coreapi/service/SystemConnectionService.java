package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.connector.ConnectorParam;
import ru.bssg.lottabyte.core.model.dataasset.DataAsset;
import ru.bssg.lottabyte.core.model.system.*;
import ru.bssg.lottabyte.core.ui.model.SearchColumn;
import ru.bssg.lottabyte.core.ui.model.SearchColumnForJoin;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.SystemConnectionRepository;
import ru.bssg.lottabyte.coreapi.repository.TaskRepository;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SystemConnectionService {
    private final SystemConnectionRepository systemConnectionRepository;
    private final SystemService systemService;
    private final ConnectorService connectorService;
    private final TaskRepository taskRepository;

    private final SearchColumn[] searchableColumns = {
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            new SearchColumn("connector.name", SearchColumn.ColumnType.Text),
            new SearchColumn("system.name", SearchColumn.ColumnType.Text),
            new SearchColumn("system_id", SearchColumn.ColumnType.UUID),
            new SearchColumn("connector_id", SearchColumn.ColumnType.UUID),
            new SearchColumn("modified", SearchColumn.ColumnType.Timestamp)
    };

    private final SearchColumnForJoin[] joinColumns = {
            new SearchColumnForJoin("name", "connector", SearchColumn.ColumnType.Text, "connector_id", "id"),
            new SearchColumnForJoin("name", "system", SearchColumn.ColumnType.Text, "system_id", "id")
    };

    public Boolean hasAccessToSystemConnection(String systemConnectionId, UserDetails userDetails) {
        return systemConnectionRepository.hasAccessToSystemConnection(systemConnectionId, userDetails);
    }

    public boolean existSystemConnectionsInSystem(String systemId, UserDetails userDetails) {
        return systemConnectionRepository.existSystemConnectionsInSystem(systemId, userDetails);
    }

    public SystemConnection createSystemConnection(UpdatableSystemConnectionEntity newSystemConnectionEntity, UserDetails userDetails) throws LottabyteException {
        if (newSystemConnectionEntity.getSystemId() == null)
            throw new LottabyteException(Message.LBE00922, userDetails.getLanguage());
        if (systemService.getSystemById(newSystemConnectionEntity.getSystemId(), userDetails) == null)
            throw new LottabyteException(Message.LBE00904, userDetails.getLanguage(), newSystemConnectionEntity.getSystemId());
        if (userDetails.getStewardId() != null && !systemService.hasAccessToSystem(newSystemConnectionEntity.getSystemId(), userDetails))
            throw new LottabyteException(Message.LBE00921, userDetails.getLanguage(), newSystemConnectionEntity.getSystemId());
        if (newSystemConnectionEntity.getConnectorId() == null || connectorService.getConnectorById(newSystemConnectionEntity.getConnectorId(), userDetails) == null)
            throw new LottabyteException(Message.LBE01101, userDetails.getLanguage(), newSystemConnectionEntity.getConnectorId());

        if(newSystemConnectionEntity.getConnectorParam() != null && !newSystemConnectionEntity.getConnectorParam().isEmpty())
            for(SystemConnectionParamEntity systemConnectionParamEntity : newSystemConnectionEntity.getConnectorParam()){
                if (systemConnectionParamEntity.getConnectorParamId() != null){
                    ConnectorParam connectorParam = connectorService.getConnectorParamById(systemConnectionParamEntity.getConnectorParamId());
                    if(connectorParam.getEntity().getRequired() && (systemConnectionParamEntity.getParamValue() == null || systemConnectionParamEntity.getParamValue().isEmpty()))
                        throw new LottabyteException(Message.LBE01204, userDetails.getLanguage(), systemConnectionParamEntity.getConnectorParamId());
                }else
                    throw new LottabyteException(Message.LBE01203, userDetails.getLanguage());
            }

        SystemConnection systemConnection = systemConnectionRepository.createSystemConnection(newSystemConnectionEntity, userDetails);
        return getSystemConnectionById(systemConnection.getId(), userDetails);
    }

    public SystemConnection patchSystemConnection(String systemConnectionId, UpdatableSystemConnectionEntity updatableSystemConnectionEntity, UserDetails userDetails) throws LottabyteException {
        SystemConnection currentSystemConnection = getSystemConnectionById(systemConnectionId, userDetails);
        if (currentSystemConnection == null)
            throw new LottabyteException(Message.LBE01201, userDetails.getLanguage(), systemConnectionId);
        if (userDetails.getStewardId() != null && !systemConnectionRepository.hasAccessToSystemConnection(systemConnectionId, userDetails))
            throw new LottabyteException(Message.LBE01208, userDetails.getLanguage(), systemConnectionId);
        if (updatableSystemConnectionEntity.getSystemId() != null) {
            if (systemService.getSystemById(updatableSystemConnectionEntity.getSystemId(), userDetails) == null)
                throw new LottabyteException(Message.LBE00904, userDetails.getLanguage(), updatableSystemConnectionEntity.getSystemId());
            if (userDetails.getStewardId() != null && !systemService.hasAccessToSystem(updatableSystemConnectionEntity.getSystemId(), userDetails))
                throw new LottabyteException(Message.LBE00921, userDetails.getLanguage(), updatableSystemConnectionEntity.getSystemId());
        }
        String connectorId = currentSystemConnection.getEntity().getConnectorId();
        if (updatableSystemConnectionEntity.getConnectorId() != null){
            if(connectorService.getConnectorById(updatableSystemConnectionEntity.getConnectorId(), userDetails) == null)
                throw new LottabyteException(Message.LBE01101, userDetails.getLanguage(), updatableSystemConnectionEntity.getConnectorId());
            connectorId = updatableSystemConnectionEntity.getConnectorId();
        }
        if(updatableSystemConnectionEntity.getConnectorParam() != null && !updatableSystemConnectionEntity.getConnectorParam().isEmpty()){
            for (SystemConnectionParamEntity systemConnectionParamEntity : updatableSystemConnectionEntity.getConnectorParam()){
                if (systemConnectionParamEntity.getConnectorParamId() == null || systemConnectionParamEntity.getConnectorParamId().isEmpty())
                    throw new LottabyteException(Message.LBE01209, userDetails.getLanguage());
                ConnectorParam connectorParam = connectorService.getConnectorParamById(systemConnectionParamEntity.getConnectorParamId());
                if (!connectorId.equals(connectorParam.getEntity().getConnectorId()))
                    throw new LottabyteException(Message.LBE01209, userDetails.getLanguage(),
                            systemConnectionParamEntity.getConnectorParamId(),
                            connectorParam.getEntity().getConnectorId(), connectorId);
                if (connectorParam.getEntity().getRequired() && (systemConnectionParamEntity.getParamValue() == null || systemConnectionParamEntity.getParamValue().isEmpty()))
                    throw new LottabyteException(Message.LBE01204, userDetails.getLanguage(), systemConnectionParamEntity.getConnectorParamId());
            }
        }
        systemConnectionRepository.patchSystemConnection(systemConnectionId, updatableSystemConnectionEntity, userDetails);
        return getSystemConnectionById(systemConnectionId, userDetails);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ArchiveResponse deleteSystemConnection(String systemConnectionId, UserDetails userDetails) throws LottabyteException {
        if (!systemConnectionRepository.existsById(systemConnectionId, userDetails))
            throw new LottabyteException(Message.LBE01201, userDetails.getLanguage(), systemConnectionId);
        if (userDetails.getStewardId() != null && !systemConnectionRepository.hasAccessToSystemConnection(systemConnectionId, userDetails))
            throw new LottabyteException(Message.LBE01208, userDetails.getLanguage(), systemConnectionId);
        if (taskRepository.existsTaskWithSystemConnection(systemConnectionId, userDetails))
            throw new LottabyteException(Message.LBE01206, userDetails.getLanguage());

        deleteSystemConnectionParamBySystemConnectionId(systemConnectionId, userDetails);
        systemConnectionRepository.deleteById(systemConnectionId, userDetails);
        ArchiveResponse archiveResponse = new ArchiveResponse();
        archiveResponse.setArchivedGuids(Collections.singletonList(systemConnectionId));
        return archiveResponse;
    }

    public ArchiveResponse deleteSystemConnectionParamBySystemConnectionId(String systemConnectionId, UserDetails userDetails) throws LottabyteException {
        SystemConnectionParam systemConnectionParam = getSystemConnectionParamBySystemConnectionId(systemConnectionId, userDetails);
        if(systemConnectionParam != null && systemConnectionParam.getEntity() != null && systemConnectionParam.getEntity().getConnectorParamId() != null){
            ConnectorParam connectorParam = connectorService.getConnectorParamById(systemConnectionParam.getEntity().getConnectorParamId());
            if(connectorParam != null && connectorParam.getEntity() != null && connectorParam.getEntity().getRequired())
                throw new LottabyteException(Message.LBE01207, userDetails.getLanguage(), systemConnectionId);
        }

        systemConnectionRepository.deleteSystemConnectionParamBySystemConnectionId(systemConnectionId, userDetails);
        ArchiveResponse archiveResponse = new ArchiveResponse();
        archiveResponse.setArchivedGuids(Collections.singletonList(systemConnectionId));
        return archiveResponse;
    }

    public SystemConnection getSystemConnectionById(String systemConnectionId, UserDetails userDetails) {
        return systemConnectionRepository.getById(systemConnectionId, userDetails);
    }

    public List<SystemConnection> getSystemConnectionsBySystemId(String systemId, UserDetails userDetails) throws LottabyteException {
        if (systemId == null || systemService.getSystemById(systemId, userDetails) == null)
            throw new LottabyteException(Message.LBE00904, userDetails.getLanguage(), systemId);
        return systemConnectionRepository.getSystemConnectionsBySystemId(systemId, userDetails);
    }

    public PaginatedArtifactList<SystemConnection> getSystemConnectionPaginated(String systemId, Integer offset, Integer limit, UserDetails userDetails) {
        return systemConnectionRepository.getAllPaginated(systemId, offset, limit, userDetails);
    }

    public SystemConnectionParam getSystemConnectionParamById(String systemConnectionParamId, UserDetails userDetails) throws LottabyteException {
        if (systemConnectionParamId == null || systemConnectionRepository.getSystemConnectionParamById(systemConnectionParamId, userDetails) == null)
            throw new LottabyteException(Message.LBE01202, userDetails.getLanguage(), systemConnectionParamId);
        return systemConnectionRepository.getSystemConnectionParamById(systemConnectionParamId, userDetails);
    }
    public SystemConnectionParam getSystemConnectionParamBySystemConnectionId(String systemConnectionId, UserDetails userDetails) {
        return systemConnectionRepository.getSystemConnectionParamBySystemConnectionId(systemConnectionId, userDetails);
    }

    public PaginatedArtifactList<SystemConnectionParam> getSystemConnectionParamsPaginated(String systemConnectionId, Integer offset, Integer limit, UserDetails userDetails) throws LottabyteException {
        if (systemConnectionId == null || getSystemConnectionById(systemConnectionId, userDetails) == null)
            throw new LottabyteException(Message.LBE01202, userDetails.getLanguage(), systemConnectionId);
        return systemConnectionRepository.getSystemConnectionParamsPaginated(systemConnectionId, offset, limit, userDetails);
    }

    public SearchResponse<FlatSystemConnection> searchSystemConnection(SearchRequestWithJoin request, UserDetails userDetails) throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        return systemConnectionRepository.searchSystemConnection(request, searchableColumns, joinColumns, userDetails);
    }

    public List<SystemConnectionParam> getSystemConnectionParamsList(String systemConnectionId, UserDetails userDetails) throws LottabyteException {
        if (systemConnectionId == null || getSystemConnectionById(systemConnectionId, userDetails) == null)
            throw new LottabyteException(Message.LBE01202, userDetails.getLanguage(), systemConnectionId);
        return systemConnectionRepository.getSystemConnectionParamsList(systemConnectionId, userDetails);
    }
}
