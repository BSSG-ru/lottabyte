package ru.bssg.lottabyte.coreapi.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.dataasset.DataAsset;
import ru.bssg.lottabyte.core.model.system.*;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.model.workflow.WorkflowTask;
import ru.bssg.lottabyte.core.model.workflow.WorkflowType;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.ConnectorRepository;
import ru.bssg.lottabyte.coreapi.repository.DataAssetRepository;
import ru.bssg.lottabyte.coreapi.repository.DomainRepository;
import ru.bssg.lottabyte.coreapi.repository.SystemRepository;
import ru.bssg.lottabyte.coreapi.util.Helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SystemService extends WorkflowableService<System> {
    private final SystemRepository systemRepository;
    private final ConnectorRepository connectorRepository;
    private final ElasticsearchService elasticsearchService;
    private final DomainRepository domainRepository;
    private final DomainService domainService;
    private final EntityService entityService;
    private final EntitySampleService entitySampleService;
    private final EntityQueryService entityQueryService;
    private final SystemConnectionService systemConnectionService;
    private final TagService tagService;
    private final CommentService commentService;
    private final CustomAttributeDefinitionService customAttributeDefinitionService;
    private final DataAssetRepository dataAssetRepository;
    private final ArtifactType serviceArtifactType = ArtifactType.system;
    private final WorkflowService workflowService;

    private final SearchColumn[] searchableColumns = {
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            new SearchColumn("domains", SearchColumn.ColumnType.Text),
            new SearchColumn("tags", SearchColumn.ColumnType.Text),
            new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
            new SearchColumn("workflow_state", SearchColumn.ColumnType.Text)
    };

    private final SearchColumnForJoin[] joinColumns = {
            new SearchColumnForJoin("domain_id", "system_to_domain", SearchColumn.ColumnType.UUID, "id", "system_id")
    };

    @Autowired
    @Lazy
    public SystemService(SystemRepository systemRepository, ElasticsearchService elasticsearchService,
            ConnectorRepository connectorRepository, DomainRepository domainRepository,
            EntityService entityService, EntitySampleService entitySampleService,
            EntityQueryService entityQueryService, SystemConnectionService systemConnectionService,
            TagService tagService, CommentService commentService,
            CustomAttributeDefinitionService customAttributeDefinitionService,
            DataAssetRepository dataAssetRepository, DomainService domainService,
            WorkflowService workflowService) {
        super(systemRepository, workflowService, tagService, ArtifactType.system, elasticsearchService);
        this.systemRepository = systemRepository;
        this.connectorRepository = connectorRepository;
        this.elasticsearchService = elasticsearchService;
        this.domainRepository = domainRepository;
        this.entityService = entityService;
        this.entitySampleService = entitySampleService;
        this.entityQueryService = entityQueryService;
        this.systemConnectionService = systemConnectionService;
        this.tagService = tagService;
        this.commentService = commentService;
        this.customAttributeDefinitionService = customAttributeDefinitionService;
        this.dataAssetRepository = dataAssetRepository;
        this.domainService = domainService;
        this.workflowService = workflowService;
    }

    // WF interface

    public boolean existsInState(String artifactId, ArtifactState artifactState, UserDetails userDetails)
            throws LottabyteException {
        System system = getSystemById(artifactId, userDetails);
        WorkflowableMetadata md = (WorkflowableMetadata) system.getMetadata();
        if (!artifactState.equals(md.getState()))
            return false;
        return true;
    }

    public String getDraftArtifactId(String publishedId, UserDetails userDetails) {
        return systemRepository.getDraftId(publishedId, userDetails);
    }

    @Override
    public String createDraft(String publishedId, WorkflowState workflowState, WorkflowType workflowType,
            UserDetails userDetails) throws LottabyteException {
        System current = getSystemById(publishedId, userDetails);
        String draftId = super.createDraft(publishedId, null, workflowState, workflowType, userDetails);
        if (current.getEntity().getDomainIds() != null && !current.getEntity().getDomainIds().isEmpty())
            for (String d : current.getEntity().getDomainIds())
                systemRepository.addSystemToDomain(draftId, d, userDetails);
        return draftId;
    }

    /*
     * public System createDraft(String publishedId, WorkflowState workflowState,
     * UserDetails userDetails) throws LottabyteException {
     * System current = getSystemById(publishedId, userDetails);
     * String draftId = createDraftSystem(current, workflowState, userDetails);
     * return getSystemById(draftId, userDetails);
     * }
     */

    public void wfCancel(String draftSystemId, UserDetails userDetails) throws LottabyteException {
        System current = systemRepository.getById(draftSystemId, userDetails);
        if (current == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftSystemId);
        if (!ArtifactState.DRAFT.equals(((WorkflowableMetadata) current.getMetadata()).getState()))
            throw new LottabyteException(
                    Message.LBE03003, userDetails.getLanguage());
        systemRepository.setStateById(draftSystemId, ArtifactState.DRAFT_HISTORY, userDetails);
    }

    public void wfApproveRemoval(String draftSystemId, UserDetails userDetails) throws LottabyteException {
        System current = systemRepository.getById(draftSystemId, userDetails);
        if (current == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftSystemId);
        String publishedId = ((WorkflowableMetadata) current.getMetadata()).getPublishedId();
        if (publishedId == null)
            throw new LottabyteException(
                    Message.LBE03006,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftSystemId);
        systemRepository.setStateById(current.getId(), ArtifactState.DRAFT_HISTORY, userDetails);
        systemRepository.setStateById(publishedId, ArtifactState.REMOVED, userDetails);
        elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(publishedId), userDetails);
    }

    public System wfPublish(String draftSystemId, UserDetails userDetails) throws LottabyteException {
        System draft = systemRepository.getById(draftSystemId, userDetails);
        String publishedId = ((WorkflowableMetadata) draft.getMetadata()).getPublishedId();
        if (draft == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftSystemId);
        /*
         * if (systemRepository.exi(draft.getEntity().getName(), publishedId,
         * userDetails))
         * throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.LBE00108,
         * draft.getEntity().getName());
         */

        if (publishedId == null) {
            String newPublishedId = systemRepository.publishSystemDraft(draftSystemId, null, userDetails);
            if (draft.getEntity().getDomainIds() != null && !draft.getEntity().getDomainIds().isEmpty())
                for (String d : draft.getEntity().getDomainIds())
                    systemRepository.addSystemToDomain(newPublishedId, d, userDetails);
            tagService.mergeTags(draftSystemId, serviceArtifactType, newPublishedId, serviceArtifactType, userDetails);
            System s = systemRepository.getById(newPublishedId, userDetails);
            elasticsearchService.insertElasticSearchEntity(
                    Collections.singletonList(getSearchableArtifact(s, userDetails)), userDetails);
            return s;
        } else {
            systemRepository.publishSystemDraft(draftSystemId, publishedId, userDetails);
            System currentPublished = systemRepository.getById(publishedId, userDetails);
            updateSystemDomains(publishedId, draft.getEntity().getDomainIds(),
                    currentPublished.getEntity().getDomainIds(), userDetails);
            tagService.mergeTags(draftSystemId, serviceArtifactType, publishedId, serviceArtifactType, userDetails);
            System s = systemRepository.getById(publishedId, userDetails);
            elasticsearchService.updateElasticSearchEntity(
                    Collections.singletonList(getSearchableArtifact(s, userDetails)), userDetails);
            return s;
        }
    }

    // System

    public System getById(String id, UserDetails userDetails) throws LottabyteException {
        return getSystemById(id, userDetails);
    }

    public System getSystemById(String systemId, UserDetails userDetails) throws LottabyteException {
        System system = systemRepository.getById(systemId, userDetails);
        if (system == null)
            throw new LottabyteException(Message.LBE00904, userDetails.getLanguage(), systemId);
        WorkflowableMetadata md = (WorkflowableMetadata) system.getMetadata();
        if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
            md.setDraftId(systemRepository.getDraftId(md.getId(), userDetails));

        system.getMetadata().setTags(tagService.getArtifactTags(systemId, userDetails));

        return system;
    }

    public System getSystemByIdAndState(String systemId, ArtifactState artifactState, UserDetails userDetails)
            throws LottabyteException {
        System system = systemRepository.getByIdAndState(systemId, artifactState.name(), userDetails);
        if (system == null)
            throw new LottabyteException(Message.LBE00904,
                            userDetails.getLanguage(), systemId);
        WorkflowableMetadata md = (WorkflowableMetadata) system.getMetadata();
        if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
            md.setDraftId(systemRepository.getDraftId(md.getId(), userDetails));

        system.getMetadata().setTags(tagService.getArtifactTags(systemId, userDetails));

        return system;
    }

    public System getSystemVersionById(String systemId, Integer versionId, UserDetails userDetails)
            throws LottabyteException {

        System system = systemRepository.getVersionById(systemId, versionId, userDetails);
        if (system == null)
            throw new LottabyteException(Message.LBE00925,
                            userDetails.getLanguage(), systemId, versionId);

        /*
         * domain.getMetadata().setTags(
         * tagService.getArtifactTags(domainId, userDetails,
         * domain.getMetadata().getEffectiveStartDate(),
         * domain.getMetadata().getEffectiveEndDate()));
         */

        fillSystemRelations(system, userDetails);
        return system;
    }

    private void fillSystemRelations(System s, UserDetails userDetails) {
        WorkflowableMetadata md = (WorkflowableMetadata) s.getMetadata();
        if (md.getAncestorDraftId() != null) {
            s.getMetadata().setTags(tagService.getArtifactTags(md.getAncestorDraftId(), userDetails));
            s.getEntity().setDomainIds(systemRepository.getDomainIdsBySystemId(md.getAncestorDraftId(), userDetails));
        }
    }

    public PaginatedArtifactList<System> getSystemVersions(String systemId, Integer offset, Integer limit,
            UserDetails userDetails) throws LottabyteException {
        if (!systemRepository.existsById(systemId,
                new ArtifactState[] { ArtifactState.PUBLISHED, ArtifactState.REMOVED }, userDetails)) {
            PaginatedArtifactList<System> res = new PaginatedArtifactList<>();
            res.setCount(0);
            res.setOffset(offset);
            res.setLimit(limit);
            res.setResources(new ArrayList<>());
            return res;
            // throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.LBE00101,
            // domainId);
        }
        /*
         * if (!systemRepository.systemExists(systemId, userDetails)) {
         * throw new LottabyteException(HttpStatus.NOT_FOUND,
         * Message.format(Message.LBE00904.getText(userDetails.getLanguage().name()),
         * userDetails.getLanguage().name(), systemId));
         * }
         */
        PaginatedArtifactList<System> systemPaginatedArtifactList = systemRepository.getVersionsById(systemId, offset,
                limit, "/v1/systems/" + systemId + "/versions", userDetails);
        for (System system : systemPaginatedArtifactList.getResources()) {
            fillSystemRelations(system, userDetails);
        }
        return systemPaginatedArtifactList;
    }

    public PaginatedArtifactList<System> getSystemsPaginated(Integer offset, Integer limit, String artifactState,
            UserDetails userDetails) throws LottabyteException {
        if (!EnumUtils.isValidEnum(ArtifactState.class, artifactState))
            throw new LottabyteException(
                    Message.LBE00067,
                            userDetails.getLanguage(),
                    artifactState);
        PaginatedArtifactList<System> systemPaginatedArtifactList = systemRepository.getAllPaginated(offset, limit, "/v1/systems",
                ArtifactState.valueOf(artifactState), userDetails);
        systemPaginatedArtifactList.getResources().forEach(
                system -> system.getMetadata().setTags(tagService.getArtifactTags(system.getId(), userDetails)));
        return systemPaginatedArtifactList;
    }

    public PaginatedArtifactList<System> getPaginatedSystemsWithoutDomain(String domainId, Integer offset,
            Integer limit, UserDetails userDetails) {
        PaginatedArtifactList<System> systemPaginatedArtifactList = systemRepository
                .getPaginatedSystemsWithoutDomain(domainId, offset, limit, userDetails);
        systemPaginatedArtifactList.getResources().forEach(
                system -> system.getMetadata().setTags(tagService.getArtifactTags(system.getId(), userDetails)));
        return systemPaginatedArtifactList;
    }

    public List<System> getSystemsByEntityId(String entityId, UserDetails userDetails) {
        return systemRepository.getSystemsByEntityId(entityId, userDetails);
    }

    public boolean hasAccessToSystem(String systemId, UserDetails userDetails) {
        return systemRepository.hasAccessToSystem(systemId, userDetails);
    }

    public void validateAccessToSystems(List<String> systemIds, UserDetails userDetails, Message message)
            throws LottabyteException {
        if (userDetails.getStewardId() != null && systemIds != null) {
            List<String> erroredSystems = new ArrayList<>();
            for (String s : systemIds) {
                if (!systemRepository.hasAccessToSystem(s, userDetails))
                    erroredSystems.add(s);
            }
            if (!erroredSystems.isEmpty())
                throw new LottabyteException(message, userDetails.getLanguage(), String.join(", ", erroredSystems));
        }
    }

    private void validateDomainIds(String systemId, UpdatableSystemEntity systemEntity, SystemEntity currentEntity,
            UserDetails userDetails) throws LottabyteException {
        if (systemEntity.getDomainIds() != null && !systemEntity.getDomainIds().isEmpty()) {
            if (systemEntity.getDomainIds().size() > 100)
                throw new LottabyteException(Message.LBE00106, userDetails.getLanguage());
            List<String> unknownIds = domainRepository.checkDomainsListExist(systemEntity.getDomainIds(), userDetails);
            if (unknownIds != null && !unknownIds.isEmpty())
                throw new LottabyteException(Message.LBE00105,
                                userDetails.getLanguage(),
                                String.join(", ", unknownIds));
        }
        List<String> validateDomainIds = new ArrayList<>();
        if (systemEntity.getDomainIds() == null) {
            if (currentEntity != null && currentEntity.getDomainIds() != null)
                validateDomainIds.addAll(currentEntity.getDomainIds());
        } else if (currentEntity == null || currentEntity.getDomainIds() == null) {
            if (systemEntity.getDomainIds() != null)
                validateDomainIds.addAll(systemEntity.getDomainIds());
        } else {
            for (String id : systemEntity.getDomainIds()) {
                if (!currentEntity.getDomainIds().contains(id))
                    validateDomainIds.add(id);
            }
            for (String id : currentEntity.getDomainIds()) {
                if (!systemEntity.getDomainIds().contains(id))
                    validateDomainIds.add(id);
            }
        }
        domainService.validateAccessToDomains(validateDomainIds, userDetails);
        if (systemId != null && currentEntity != null && systemEntity.getDomainIds() != null) {
            List<String> domainIdsToDelete = currentEntity.getDomainIds().stream()
                    .filter(x -> !systemEntity.getDomainIds().contains(x))
                    .collect(Collectors.toList());
            domainService.validateAccessToDomains(domainIdsToDelete, userDetails);
            if (domainIdsToDelete.stream()
                    .anyMatch(x -> dataAssetRepository.existsDataAssetWithSystemAndDomain(systemId, x, userDetails))) {
                List<String> errors = new ArrayList<>();
                List<String> erroredDomainsIds = new ArrayList<>();
                for (String domainId : domainIdsToDelete) {
                    List<DataAsset> dataAssets = dataAssetRepository.getDataAssetsBySystemAndDomain(systemId, domainId,
                            userDetails);
                    if (!dataAssets.isEmpty()) {
                        dataAssets.stream().forEach(x -> errors.add(x.getId() + " (" + x.getName() + ")"));
                        erroredDomainsIds.add(domainId);
                    }
                }
                throw new LottabyteException(Message.LBE00917,
                                userDetails.getLanguage(),
                                String.join(", ", erroredDomainsIds), String.join(", ", errors));
            }
        }
    }

    public System createSystem(UpdatableSystemEntity newSystemEntity, UserDetails userDetails)
            throws LottabyteException {
        if (newSystemEntity.getName() == null || newSystemEntity.getName().isEmpty())
            throw new LottabyteException(
                    Message.LBE00906, userDetails.getLanguage());
        if (newSystemEntity.getSystemType() == null || newSystemEntity.getSystemType().isEmpty())
            throw new LottabyteException(
                    Message.LBE00907, userDetails.getLanguage());
        if (newSystemEntity.getConnectorId() != null
                && connectorRepository.getById(newSystemEntity.getConnectorId(), null) == null)
            throw new LottabyteException(Message.LBE01101,
                            userDetails.getLanguage(), newSystemEntity.getConnectorId());
        if (newSystemEntity.getSystemFolderId() != null
                && getSystemFolderById(newSystemEntity.getSystemFolderId(), false, userDetails) == null)
            throw new LottabyteException(Message.LBE00902,
                            userDetails.getLanguage(), newSystemEntity.getSystemFolderId());
        if (!systemRepository.existSystemTypeByName(newSystemEntity.getSystemType(), userDetails))
            throw new LottabyteException(Message.LBE00905,
                            userDetails.getLanguage(), newSystemEntity.getSystemType());
        if (systemRepository.existsSystemInFolder(newSystemEntity.getName(), newSystemEntity.getSystemFolderId(), null,
                userDetails))
            throw new LottabyteException(Message.LBE00901,
                            userDetails.getLanguage(), newSystemEntity.getName());
        validateDomainIds(null, newSystemEntity, null, userDetails);

        String workflowTaskId = null;
        ProcessInstance pi = null;
        newSystemEntity.setId(UUID.randomUUID().toString());
        if (workflowService.isWorkflowEnabled(serviceArtifactType)
                && workflowService.getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

            pi = workflowService.startFlowableProcess(newSystemEntity.getId(), serviceArtifactType,
                    ArtifactAction.CREATE,
                    userDetails);
            workflowTaskId = pi.getId();

        }

        String newSystemId = systemRepository.createSystem(newSystemEntity,
                workflowTaskId, userDetails);

        System system = getSystemById(newSystemId, userDetails);

        return system;
    }

    public System patchSystem(String systemId, UpdatableSystemEntity systemEntity, UserDetails userDetails)
            throws LottabyteException {
        System current = systemRepository.getById(systemId, userDetails);
        if (current == null)
            throw new LottabyteException(Message.LBE00904,
                            userDetails.getLanguage(), systemId);
        if (userDetails.getStewardId() != null && !systemRepository.hasAccessToSystem(systemId, userDetails))
            throw new LottabyteException(
                    Message.LBE00919,
                            userDetails.getLanguage(),
                    systemId);

        String draftId = null;
        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            draftId = systemRepository.getDraftId(systemId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE00924,
                                userDetails.getLanguage(),
                        draftId);
        }

        if (systemEntity.getName() != null && systemEntity.getName().isEmpty())
            throw new LottabyteException(
                    Message.LBE00906, userDetails.getLanguage());
        if (systemEntity.getSystemType() != null && systemEntity.getSystemType().isEmpty())
            throw new LottabyteException(
                    Message.LBE00907, userDetails.getLanguage());
        if (systemEntity.getConnectorId() != null && !systemEntity.getConnectorId().isEmpty() &&
                !connectorRepository.existsById(systemEntity.getConnectorId(), null))
            throw new LottabyteException(Message.LBE01101,
                            userDetails.getLanguage(), systemEntity.getConnectorId());
        if (systemEntity.getSystemFolderId() != null && !systemEntity.getSystemFolderId().isEmpty() &&
                getSystemFolderById(systemEntity.getSystemFolderId(), false, userDetails) == null)
            throw new LottabyteException(Message.LBE00902,
                            userDetails.getLanguage(), systemEntity.getSystemFolderId());
        if (systemEntity.getSystemType() != null
                && !systemRepository.existSystemTypeByName(systemEntity.getSystemType(), userDetails))
            throw new LottabyteException(Message.LBE00905,
                            userDetails.getLanguage(), systemEntity.getSystemType());

        validateDomainIds(systemId, systemEntity, current.getEntity(), userDetails);

        ProcessInstance pi = null;
        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            String workflowTaskId = null;
            draftId = UUID.randomUUID().toString();
            if (workflowService.isWorkflowEnabled(serviceArtifactType) && workflowService
                    .getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

                pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.UPDATE,
                        userDetails);
                workflowTaskId = pi.getId();

            }
            systemRepository.createDraftFromPublished(systemId, draftId, workflowTaskId, userDetails);

            if (current.getEntity().getDomainIds() != null && !current.getEntity().getDomainIds().isEmpty())
                for (String d : current.getEntity().getDomainIds())
                    systemRepository.addSystemToDomain(draftId, d, userDetails);
            tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

        } else {
            draftId = systemId;


        }

        systemRepository.patchSystem(draftId, systemEntity, userDetails);
        System system = getSystemById(draftId, userDetails);
        // elasticsearchService.updateElasticSearchEntity(Collections.singletonList(system.getSearchableArtifact()),
        // userDetails);
        return system;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public System deleteSystem(String systemId, UserDetails userDetails) throws LottabyteException {
        if (!systemRepository.existsByIdAndPublished(systemId, userDetails))
            throw new LottabyteException(
                    Message.LBE00904,
                            userDetails.getLanguage(),
                    systemId);
        if (userDetails.getStewardId() != null && !systemRepository.hasAccessToSystem(systemId, userDetails))
            throw new LottabyteException(
                    Message.LBE00919,
                            userDetails.getLanguage(),
                    systemId);

        System current = systemRepository.getById(systemId, userDetails);
        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            // if (entityService.existEntitiesInSystem(systemId, userDetails))//при
            // появлении черновика старые связи остаются для черновиков, например, поэтому
            // эта проверка больше не имеет смысла, либо удалить, либо добавить проверку на
            // PUBLISHED основной сущности
            // throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.LBE00908);
            if (entitySampleService.existSamplesInSystem(systemId, userDetails))
                throw new LottabyteException(
                        Message.LBE00909, userDetails.getLanguage());
            if (entityQueryService.existQueriesInSystem(systemId, userDetails))
                throw new LottabyteException(
                        Message.LBE00910, userDetails.getLanguage());
            if (systemConnectionService.existSystemConnectionsInSystem(systemId, userDetails))
                throw new LottabyteException(
                        Message.LBE00911, userDetails.getLanguage());
            if (dataAssetRepository.existsDataAssetWithSystem(systemId, userDetails))
                throw new LottabyteException(
                        Message.LBE00918, userDetails.getLanguage());

            String draftId = systemRepository.getDraftId(systemId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE00924,
                                userDetails.getLanguage(),
                        draftId);
            draftId = createDraftSystem(current, WorkflowState.MARKED_FOR_REMOVAL, userDetails);
            return systemRepository.getById(draftId, userDetails);
        } else {
            systemRepository.removeSystemFromAllDomains(systemId, userDetails);
            tagService.deleteAllTagsByArtifactId(systemId, userDetails);
            commentService.deleteAllCommentsByArtifactId(systemId, userDetails);
            customAttributeDefinitionService.deleteAllCustomAttributesByArtifactId(systemId, userDetails);
            systemRepository.deleteById(systemId, userDetails);
            return null;
        }
    }

    // -------------
    // Folders
    // -------------

    public SystemFolder getSystemFolderById(String systemFolderId, Boolean includeChildren, UserDetails userDetails) {
        return systemRepository.getSystemFolderById(systemFolderId, includeChildren, userDetails);
    }

    public List<SystemFolder> getRootFolders(Boolean includeChildren, UserDetails userDetails) {
        return systemRepository.getRootFolders(includeChildren, userDetails);
    }

    public SystemFolder createFolder(UpdatableSystemFolderEntity newSystemFolderEntity, UserDetails userDetails)
            throws LottabyteException {
        if (newSystemFolderEntity.getName() == null || newSystemFolderEntity.getName().isEmpty())
            throw new LottabyteException(
                    Message.LBE00913, userDetails.getLanguage());
        if (newSystemFolderEntity.getParentId() != null &&
                systemRepository.getSystemFolderById(newSystemFolderEntity.getParentId(), false, userDetails) == null) {
            throw new LottabyteException(
                    Message.LBE00903,
                            userDetails.getLanguage(),
                    newSystemFolderEntity.getParentId());
        }
        if (systemRepository.existsSubfolder(newSystemFolderEntity.getName(), newSystemFolderEntity.getParentId(),
                userDetails))
            throw new LottabyteException(
                    Message.LBE00914,
                            userDetails.getLanguage(),
                    newSystemFolderEntity.getName());

        String folderId = systemRepository.createFolder(newSystemFolderEntity, userDetails);
        SystemFolder systemFolder = getSystemFolderById(folderId, false, userDetails);
        elasticsearchService.insertElasticSearchEntity(
                Collections.singletonList(getFolderSearchableArtifact(systemFolder, userDetails)), userDetails);
        return systemFolder;
    }

    public SystemFolder patchFolder(String folderId, UpdatableSystemFolderEntity systemFolderEntity,
            UserDetails userDetails) throws LottabyteException {
        SystemFolder currentSystemFolder = getSystemFolderById(folderId, false, userDetails);
        if (systemFolderEntity.getParentId() != null
                && getSystemFolderById(systemFolderEntity.getParentId(), false, userDetails) == null) {
            throw new LottabyteException(
                    Message.LBE00903,
                            userDetails.getLanguage(),
                    systemFolderEntity.getParentId());
        }
        if ((systemFolderEntity.getName() != null || systemFolderEntity.getParentId() != null) &&
                systemRepository.existsSubfolder(
                        systemFolderEntity.getName() != null ? systemFolderEntity.getName()
                                : currentSystemFolder.getName(),
                        systemFolderEntity.getParentId() != null ? systemFolderEntity.getParentId()
                                : currentSystemFolder.getEntity().getParentId(),
                        userDetails))
            throw new LottabyteException(
                    Message.LBE00914,
                            userDetails.getLanguage(),
                    systemFolderEntity.getName() != null ? systemFolderEntity.getName()
                            : currentSystemFolder.getName());

        systemRepository.patchFolder(folderId, systemFolderEntity, userDetails);
        SystemFolder systemFolder = getSystemFolderById(folderId, false, userDetails);
        elasticsearchService.updateElasticSearchEntity(
                Collections.singletonList(getFolderSearchableArtifact(systemFolder, userDetails)), userDetails);
        return systemFolder;
    }

    public ArchiveResponse deleteFolder(String folderId, UserDetails userDetails) throws LottabyteException {
        if (folderId != null && getSystemFolderById(folderId, false, userDetails) == null) {
            throw new LottabyteException(
                    Message.LBE00902,
                            userDetails.getLanguage(),
                    folderId);
        }
        ArchiveResponse archiveResponse = new ArchiveResponse();
        List<SystemFolder> systemFolderList = getSystemFolderWithAllChildrenById(folderId, userDetails);
        if (systemFolderList != null && !systemFolderList.isEmpty()) {
            for (SystemFolder systemFolder : systemFolderList) {
                System system = getSystemBySystemFolderId(systemFolder.getId(), userDetails);
                if (system != null && system.getId() != null)
                    throw new LottabyteException(
                            Message.LBE00916,
                                    userDetails.getLanguage(),
                            folderId, system.getId());
            }
            systemRepository.recursiveDeletionFolders(systemFolderList, userDetails);
            List<String> idList = systemFolderList.stream().map(ModeledObject::getId).collect(Collectors.toList());
            elasticsearchService.deleteElasticSearchEntityById(idList, userDetails);
            archiveResponse.setArchivedGuids(idList);
        }

        return archiveResponse;
    }

    public System getSystemBySystemFolderId(String systemFolderId, UserDetails userDetails) {
        return systemRepository.getSystemBySystemFolderId(systemFolderId, userDetails);
    }

    public List<SystemFolder> getSystemFolderWithAllChildrenById(String systemFolderId, UserDetails userDetails) {
        return systemRepository.getSystemFolderWithAllChildrenById(systemFolderId, userDetails);
    }

    public SystemFolder getSystemFolderByName(String systemFolderName, Boolean includeChildren,
            UserDetails userDetails) {
        return systemRepository.getSystemFolderByName(systemFolderName, includeChildren, userDetails);
    }

    // -------------
    // Search
    // -------------

    public SearchResponse<FlatSystem> searchSystems(SearchRequestWithJoin request, UserDetails userDetails)
            throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        SearchResponse<FlatSystem> res = systemRepository.searchSystems(request, searchableColumns, joinColumns,
                userDetails);
        res.getItems().stream().forEach(
                x -> x.setTags(tagService.getArtifactTags(x.getId(), userDetails)
                        .stream().map(y -> y.getName()).collect(Collectors.toList())));
        res.getItems().stream()
                .filter(x -> ArtifactState.DRAFT.equals(x.getState()) && x.getWorkflowTaskId() != null)
                .forEach(y -> {
                    WorkflowTask task = workflowService.getWorkflowTaskById(y.getWorkflowTaskId(), userDetails, false);
                    if (task != null)
                        y.setWorkflowState(task.getEntity().getWorkflowState());
                });

        res.getItems()
                .forEach(s -> s.setDomains(domainService.getDomainsBySystemId(s.getId(), userDetails).stream()
                        .map(x -> FlatRelation.builder()
                                .id(x.getId()).name(x.getName()).url("/v1/domains/" + x.getId()).build())
                        .collect(Collectors.toList())));
        return res;
    }

    public Boolean existSystemById(String systemId, UserDetails userDetails) {
        return systemRepository.existsByIdAndPublished(systemId, userDetails);
    }

    public Boolean allSystemsExist(List<String> systemIds, UserDetails userDetails) {
        return systemRepository.allSystemsExist(systemIds, userDetails);
    }

    // -------------
    // Types
    // -------------

    public List<SystemType> getSystemTypes(UserDetails userDetails) {
        return systemRepository.getSystemTypes(userDetails);
    }

    public List<SystemType> getSystemTypesFromDaSchema() {
        return systemRepository.getSystemTypesFromDaSchema();
    }

    public String createSystemType(SystemType systemType, UserDetails userDetails) {
        return systemRepository.createSystemType(systemType, userDetails);
    }

    private void updateSystemDomains(String systemId, List<String> ids, List<String> currentIds,
            UserDetails userDetails) {
        ids.stream().filter(x -> !currentIds.contains(x)).collect(Collectors.toList())
                .forEach(y -> domainRepository.addSystemToDomain(systemId, y, userDetails));
        currentIds.stream().filter(x -> !ids.contains(x)).collect(Collectors.toList())
                .forEach(y -> domainRepository.removeSystemFromDomain(systemId, y, userDetails));
    }

    private String createDraftSystem(System current, WorkflowState workflowState, UserDetails userDetails) {
        String workflowTaskId = workflowService.getNewWorkflowTaskUUID().toString();
        String draftId = systemRepository.createSystemDraft(current.getId(), workflowTaskId, userDetails);
        if (current.getEntity().getDomainIds() != null && !current.getEntity().getDomainIds().isEmpty())
            for (String d : current.getEntity().getDomainIds())
                systemRepository.addSystemToDomain(draftId, d, userDetails);
        tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);
        workflowService.postCreateDraft(workflowTaskId, WorkflowType.REMOVE, draftId, serviceArtifactType,
                workflowState, userDetails);
        return draftId;
    }

    public SearchableSystem getSearchableArtifact(System system, UserDetails userDetails) {
        SearchableSystem sa = SearchableSystem.builder()
            .id(system.getMetadata().getId())
            .versionId(system.getMetadata().getVersionId())
            .name(system.getMetadata().getName())
            .description(system.getEntity().getDescription())
            .modifiedBy(system.getMetadata().getModifiedBy())
            .modifiedAt(system.getMetadata().getModifiedAt())
            .artifactType(system.getMetadata().getArtifactType())
            .effectiveStartDate(system.getMetadata().getEffectiveStartDate())
            .effectiveEndDate(system.getMetadata().getEffectiveEndDate())
            .tags(Helper.getEmptyListIfNull(system.getMetadata().getTags()).stream()
                    .map(x -> x.getName()).collect(Collectors.toList()))

            .domains(systemRepository.getDomainIdsBySystemId(system.getId(), userDetails))

            .systemType(system.getEntity().getSystemType())
            .connectorId(system.getEntity().getConnectorId())
            .systemFolderId(system.getEntity().getSystemFolderId()).build();
        return sa;
    }

    public SearchableSystemFolder getFolderSearchableArtifact(SystemFolder systemFolder, UserDetails userDetails) {
        SearchableSystemFolder sa = SearchableSystemFolder.builder()
            .id(systemFolder.getMetadata().getId())
            .versionId(systemFolder.getMetadata().getVersionId())
            .name(systemFolder.getMetadata().getName())
            .description(systemFolder.getEntity().getDescription())
            .modifiedBy(systemFolder.getMetadata().getModifiedBy())
            .modifiedAt(systemFolder.getMetadata().getModifiedAt())
            .artifactType(systemFolder.getMetadata().getArtifactType())
            .effectiveStartDate(systemFolder.getMetadata().getEffectiveStartDate())
            .effectiveEndDate(systemFolder.getMetadata().getEffectiveEndDate())
            .tags(Helper.getEmptyListIfNull(systemFolder.getMetadata().getTags()).stream()
                    .map(x -> x.getName()).collect(Collectors.toList()))

            .parentId(systemFolder.getEntity().getParentId())
            .children(systemFolder.getEntity().getChildren()).build();
        return sa;
    }
}
