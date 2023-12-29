package ru.bssg.lottabyte.coreapi.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.businessEntity.BusinessEntity;
import ru.bssg.lottabyte.core.model.dataentity.DataEntity;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;
import ru.bssg.lottabyte.core.model.entityQuery.FlatEntityQuery;
import ru.bssg.lottabyte.core.model.entityQuery.SearchableEntityQuery;
import ru.bssg.lottabyte.core.model.entityQuery.UpdatableEntityQueryEntity;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.model.workflow.WorkflowTask;
import ru.bssg.lottabyte.core.model.workflow.WorkflowType;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.DomainRepository;
import ru.bssg.lottabyte.coreapi.repository.EntityQueryRepository;
import ru.bssg.lottabyte.coreapi.util.Helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EntityQueryService extends WorkflowableService<EntityQuery> {
    private final EntityQueryRepository entityQueryRepository;
    private final EntitySampleService entitySampleService;
    private final TaskService taskService;
    private final EntityService entityService;
    private final SystemService systemService;
    private final ElasticsearchService elasticsearchService;
    private final TagService tagService;
    private final CommentService commentService;
    private final DomainRepository domainRepository;
    private final RatingService ratingService;
    private final WorkflowService workflowService;
    private final ArtifactType serviceArtifactType = ArtifactType.entity_query;

    private final SearchColumn[] searchableColumns = {
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("entity_id", SearchColumn.ColumnType.UUID),
            new SearchColumn("system_id", SearchColumn.ColumnType.UUID),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            new SearchColumn("system.name", SearchColumn.ColumnType.Text),
            new SearchColumn("entity.name", SearchColumn.ColumnType.Text),
            new SearchColumn("tags", SearchColumn.ColumnType.Text),
            new SearchColumn("workflow_state", SearchColumn.ColumnType.Text),
            new SearchColumn("modified", SearchColumn.ColumnType.Timestamp)
    };

    private final SearchColumnForJoin[] joinColumns = {
            new SearchColumnForJoin("domain_id", "system_to_domain", SearchColumn.ColumnType.UUID, "id", "system_id")
    };

    @Autowired
    @Lazy
    public EntityQueryService(EntityQueryRepository entityQueryRepository, EntitySampleService entitySampleService,
            TaskService taskService, EntityService entityService, SystemService systemService,
            ElasticsearchService elasticsearchService, TagService tagService,
            CommentService commentService, DomainRepository domainRepository,
            RatingService ratingService, WorkflowService workflowService) {
        super(entityQueryRepository, workflowService, tagService, ArtifactType.entity_query, elasticsearchService);
        this.entityQueryRepository = entityQueryRepository;
        this.entitySampleService = entitySampleService;
        this.taskService = taskService;
        this.entityService = entityService;
        this.systemService = systemService;
        this.elasticsearchService = elasticsearchService;
        this.tagService = tagService;
        this.commentService = commentService;
        this.domainRepository = domainRepository;
        this.ratingService = ratingService;
        this.workflowService = workflowService;
    }

    public EntityQuery wfPublish(String draftEntityQueryId, UserDetails userDetails) throws LottabyteException {
        EntityQuery draft = entityQueryRepository.getById(draftEntityQueryId, userDetails);
        String publishedId = ((WorkflowableMetadata) draft.getMetadata()).getPublishedId();
        if (draft == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftEntityQueryId);
        // TODO: Validate name unuqieness?
        EntityQuery entityQuery;

        if (publishedId == null) {
            publishedId = entityQueryRepository.publishDraft(draftEntityQueryId, null, userDetails);
            tagService.mergeTags(draftEntityQueryId, serviceArtifactType, publishedId, serviceArtifactType,
                    userDetails);
            entityQuery = getEntityQueryById(publishedId, userDetails);
            elasticsearchService.insertElasticSearchEntity(
                    Collections.singletonList(getSearchableArtifact(entityQuery, userDetails)), userDetails);
        } else {
            entityQueryRepository.publishDraft(draftEntityQueryId, publishedId, userDetails);
            tagService.mergeTags(draftEntityQueryId, serviceArtifactType, publishedId, serviceArtifactType,
                    userDetails);
            entityQuery = getEntityQueryById(publishedId, userDetails);
            elasticsearchService.updateElasticSearchEntity(
                    Collections.singletonList(getSearchableArtifact(entityQuery, userDetails)), userDetails);
        }
        return entityQuery;
    }

    public void wfApproveRemoval(String draftSystemId, UserDetails userDetails) throws LottabyteException {
        EntityQuery entityQuery = getEntityQueryById(draftSystemId, userDetails);
        if (entityQuery == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftSystemId);
        String publishedId = ((WorkflowableMetadata) entityQuery.getMetadata()).getPublishedId();
        if (publishedId == null)
            throw new LottabyteException(
                    Message.LBE03006,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftSystemId);
        entityQueryRepository.setStateById(entityQuery.getId(), ArtifactState.DRAFT_HISTORY, userDetails);
        entityQueryRepository.setStateById(publishedId, ArtifactState.REMOVED, userDetails);
        elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(publishedId), userDetails);
    }

    public EntityQuery getById(String id, UserDetails userDetails) throws LottabyteException {
        return getEntityQueryById(id, userDetails);
    }

    public boolean existQueriesInSystem(String systemId, UserDetails userDetails) {
        return entityQueryRepository.existQueriesInSystem(systemId, userDetails);
    }

    public boolean hasAccessToQuery(String queryId, UserDetails userDetails) {
        return entityQueryRepository.hasAccessToQuery(queryId, userDetails);
    }

    public PaginatedArtifactList<EntityQuery> getAllEntityQueriesPaginated(Integer offset, Integer limit,
            String artifactState, UserDetails userDetails) throws LottabyteException {
        if (!EnumUtils.isValidEnum(ArtifactState.class, artifactState))
            throw new LottabyteException(
                    Message.LBE00067,
                            userDetails.getLanguage(),
                    artifactState);
        PaginatedArtifactList<EntityQuery> res = entityQueryRepository.getAllPaginated(offset, limit, "/v1/queries/",
                ArtifactState.valueOf(artifactState), userDetails);
        res.getResources()
                .forEach(query -> query.getMetadata().setTags(tagService.getArtifactTags(query.getId(), userDetails)));
        return res;
    }

    public PaginatedArtifactList<EntityQuery> getAllQueryEntitiesPaginated(String entityId, Integer offset,
            Integer limit, UserDetails userDetails) throws LottabyteException {
        if (entityId == null || !entityService.existEntityByIdAndState(entityId, userDetails)) {
            throw new LottabyteException(
                    Message.LBE00301,
                            userDetails.getLanguage(),
                    entityId);
        }

        return entityQueryRepository.getAllQueryEntitiesPaginated(entityId, offset, limit, userDetails);
    }

    /*
     * public EntityQuery getQueryById(String queryId, UserDetails userDetails)
     * throws LottabyteException {
     * EntityQuery entityQuery = entityQueryRepository.getById(queryId,
     * userDetails);
     * if (entityQuery == null)
     * throw new LottabyteException(HttpStatus.NOT_FOUND, Message.LBE00401,
     * queryId);
     * entityQuery.getMetadata().setTags(tagService.getArtifactTags(queryId,
     * userDetails));
     * WorkflowableMetadata md = (WorkflowableMetadata)entityQuery.getMetadata();
     * if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
     * md.setDraftId(entityQueryRepository.getDraftId(md.getId(), userDetails));
     * return entityQuery;
     * }
     */

    public EntityQuery createQuery(UpdatableEntityQueryEntity newEntityQueryEntity, UserDetails userDetails)
            throws LottabyteException {
        if (newEntityQueryEntity.getName() == null || newEntityQueryEntity.getName().isEmpty())
            throw new LottabyteException(
                    Message.LBE00404, userDetails.getLanguage());
        if (newEntityQueryEntity.getQueryText() == null || newEntityQueryEntity.getQueryText().isEmpty()) {
            throw new LottabyteException(
                    Message.LBE00405, userDetails.getLanguage());
        }
        validateEntityAndSystem(newEntityQueryEntity, userDetails);

        String workflowTaskId = null;
        ProcessInstance pi = null;

        newEntityQueryEntity.setId(UUID.randomUUID().toString());

        if (workflowService.isWorkflowEnabled(serviceArtifactType)
                && workflowService.getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

            pi = workflowService.startFlowableProcess(newEntityQueryEntity.getId(), serviceArtifactType,
                    ArtifactAction.CREATE,
                    userDetails);
            workflowTaskId = pi.getId();

        }

        String newQueryId = entityQueryRepository.createQuery(newEntityQueryEntity, workflowTaskId, userDetails);
        EntityQuery result = getEntityQueryById(newQueryId, userDetails);

        return result;
    }

    private void validateEntityAndSystem(UpdatableEntityQueryEntity entityQueryEntity, UserDetails userDetails)
            throws LottabyteException {
        if (entityQueryEntity.getEntityId() != null) {
            if (!entityService.existEntityByIdAndState(entityQueryEntity.getEntityId(), userDetails))
                throw new LottabyteException(Message.LBE00401,
                                userDetails.getLanguage(), entityQueryEntity.getEntityId());
            if (userDetails.getStewardId() != null
                    && !entityService.hasAccessToEntity(entityQueryEntity.getEntityId(), userDetails))
                throw new LottabyteException(Message.LBE00324,
                                userDetails.getLanguage(), entityQueryEntity.getEntityId());
        }
        if (entityQueryEntity.getSystemId() != null) {
            if (!systemService.existSystemById(entityQueryEntity.getSystemId(), userDetails))
                throw new LottabyteException(Message.LBE00904,
                                userDetails.getLanguage(), entityQueryEntity.getSystemId());
            if (userDetails.getStewardId() != null
                    && !systemService.hasAccessToSystem(entityQueryEntity.getSystemId(), userDetails))
                throw new LottabyteException(Message.LBE00921,
                                userDetails.getLanguage(), entityQueryEntity.getSystemId());
        }
    }

    public EntityQuery patchQuery(String queryId, UpdatableEntityQueryEntity entityQueryEntity, UserDetails userDetails)
            throws LottabyteException {
        EntityQuery current = entityQueryRepository.getById(queryId, userDetails);
        if (current == null)
            throw new LottabyteException(
                    Message.LBE00401,
                            userDetails.getLanguage(),
                    queryId);
        String draftId = null;
        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            // draftId = dataAssetRepository.getDraftDataAssetId(dataAssetId, userDetails);
            draftId = entityQueryRepository.getDraftId(queryId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE00505,
                                userDetails.getLanguage(),
                        draftId);
        }

        if (userDetails.getStewardId() != null && !entityQueryRepository.hasAccessToQuery(queryId, userDetails))
            throw new LottabyteException(
                    Message.LBE00403,
                            userDetails.getLanguage(),
                    queryId);
        if (entityQueryEntity.getName() != null && entityQueryEntity.getName().isEmpty())
            throw new LottabyteException(
                    Message.LBE00404, userDetails.getLanguage());
        if (entityQueryEntity.getQueryText() != null && entityQueryEntity.getQueryText().isEmpty()) {
            throw new LottabyteException(
                    Message.LBE00405, userDetails.getLanguage());
        }
        validateEntityAndSystem(entityQueryEntity, userDetails);

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
            entityQueryRepository.createDraftFromPublished(queryId, draftId, workflowTaskId, userDetails);
            tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

        } else {
            draftId = queryId;
        }

        entityQueryRepository.patchQuery(draftId, entityQueryEntity, userDetails);
        EntityQuery entityQuery = getEntityQueryById(draftId, userDetails);
        // elasticsearchService.updateElasticSearchEntity(Collections.singletonList(entityQuery.getSearchableArtifact()),
        // userDetails);
        return entityQuery;
    }

    public EntityQuery deleteQuery(String queryId, UserDetails userDetails) throws LottabyteException {
        EntityQuery current = getEntityQueryById(queryId, userDetails);
        if (userDetails.getStewardId() != null && !entityQueryRepository.hasAccessToQuery(queryId, userDetails))
            throw new LottabyteException(Message.LBE00403,
                            userDetails.getLanguage(), queryId);
        if (entitySampleService.getEntitySampleByQueryId(queryId, false, userDetails) != null)
            throw new LottabyteException(Message.LBE00320,
                            userDetails.getLanguage(), queryId);
        if (!taskService.getTasksByQueryId(queryId, 1, 0, userDetails).getResources().isEmpty())
            throw new LottabyteException(
                    Message.LBE00321, userDetails.getLanguage());

        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            String draftId = entityQueryRepository.getDraftId(queryId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE00406,
                                userDetails.getLanguage(),
                        draftId);

            ProcessInstance pi = null;
            draftId = null;
            String workflowTaskId = null;

            draftId = UUID.randomUUID().toString();
            pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.REMOVE, userDetails);
            workflowTaskId = pi.getId();

            entityQueryRepository.createDraftFromPublished(current.getId(), draftId, workflowTaskId, userDetails);
            return getEntityQueryById(draftId, userDetails);
        } else {
            tagService.deleteAllTagsByArtifactId(queryId, userDetails);
            entityQueryRepository.deleteById(queryId, userDetails);
            taskService.deleteTasksByQueryId(queryId, userDetails);
            return null;
        }
        /*
         * ArchiveResponse archiveResponse = new ArchiveResponse();
         * entityQueryRepository.deleteById(queryId, userDetails);
         * tagService.deleteAllTagsByArtifactId(queryId, userDetails);
         * commentService.deleteAllCommentsByArtifactId(queryId, userDetails);
         * ratingService.removeArtifactRate(queryId, userDetails);
         * List<String> idList = new ArrayList<>(Collections.singletonList(queryId));
         * elasticsearchService.deleteElasticSearchEntityById(idList, userDetails);
         * archiveResponse.setArchivedGuids(idList);
         * 
         * return archiveResponse;
         */
    }

    public EntityQuery getEntityQueryById(String queryId, UserDetails userDetails) throws LottabyteException {
        EntityQuery entityQuery = entityQueryRepository.getById(queryId, userDetails);
        if (entityQuery == null)
            throw new LottabyteException(
                    Message.LBE00401,
                            userDetails.getLanguage(),
                    queryId);
        entityQuery.getMetadata().setTags(tagService.getArtifactTags(queryId, userDetails));
        WorkflowableMetadata md = (WorkflowableMetadata) entityQuery.getMetadata();
        if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
            md.setDraftId(entityQueryRepository.getDraftId(md.getId(), userDetails));
        return entityQuery;
    }

    public EntityQuery getEntityQueryByIdAndStation(String queryId, ArtifactState artifactState,
            UserDetails userDetails) throws LottabyteException {
        EntityQuery entityQuery = entityQueryRepository.getByIdAndStation(queryId, artifactState, userDetails);
        if (entityQuery == null)
            throw new LottabyteException(
                    Message.LBE00401,
                            userDetails.getLanguage(),
                    queryId);
        entityQuery.getMetadata().setTags(tagService.getArtifactTags(queryId, userDetails));
        WorkflowableMetadata md = (WorkflowableMetadata) entityQuery.getMetadata();
        if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
            md.setDraftId(entityQueryRepository.getDraftId(md.getId(), userDetails));
        return entityQuery;
    }

    public EntityQuery getEntityQueryByEntityIdAndSystemId(String entityId, String systemId, UserDetails userDetails) {
        return entityQueryRepository.getEntityQueryByEntityIdAndSystemId(entityId, systemId, userDetails);
    }

    public List<EntityQuery> getEntityQueryListByEntityId(String entityId, UserDetails userDetails) {
        return entityQueryRepository.getEntityQueryListByEntityId(entityId, userDetails);
    }

    public SearchResponse<FlatEntityQuery> searchEntityQuery(SearchRequestWithJoin request, UserDetails userDetails)
            throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        SearchResponse<FlatEntityQuery> res = entityQueryRepository.searchEntityQuery(request, searchableColumns,
                joinColumns, userDetails);
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
        return res;
    }

    public SearchResponse<FlatEntityQuery> searchEntityQueryByDomain(SearchRequestWithJoin request, String domainId,
            UserDetails userDetails) throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        if (domainId == null || domainId.isEmpty() || !domainId
                .matches("[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}")) {
            throw new LottabyteException(Message.LBE00101,
                            userDetails.getLanguage(), domainId);
        }
        if (request.getLimitSteward() != null && request.getLimitSteward() && userDetails.getStewardId() != null &&
                !domainRepository.hasAccessToDomain(domainId, userDetails))
            return new SearchResponse<>(0, request.getLimit(), request.getOffset(), new ArrayList<>());

        SearchResponse<FlatEntityQuery> res = entityQueryRepository.searchEntityQueryByDomain(request, domainId,
                searchableColumns, joinColumns, userDetails);
        res.getItems().stream()
                .filter(x -> ArtifactState.DRAFT.equals(x.getState()) && x.getWorkflowTaskId() != null)
                .forEach(y -> {
                    WorkflowTask task = workflowService.getWorkflowTaskById(y.getWorkflowTaskId(), userDetails, false);
                    if (task != null)
                        y.setWorkflowState(task.getEntity().getWorkflowState());
                });
        return res;
    }

    public PaginatedArtifactList<EntityQuery> getEntityQueryVersionsById(String queryId, Integer offset, Integer limit,
            UserDetails userDetails) throws LottabyteException {
        if (!entityQueryRepository.existsById(queryId,
                new ArtifactState[] { ArtifactState.PUBLISHED, ArtifactState.REMOVED }, userDetails)) {
            // throw new LottabyteException(HttpStatus.NOT_FOUND,
            // Message.format(Message.LBE00503, assetId));
            PaginatedArtifactList<EntityQuery> res = new PaginatedArtifactList<>();
            res.setCount(0);
            res.setOffset(offset);
            res.setLimit(limit);
            res.setResources(new ArrayList<>());
            return res;
        }
        PaginatedArtifactList<EntityQuery> res = entityQueryRepository.getVersionsById(queryId, offset, limit,
                "/v1/queries", userDetails);
        for (EntityQuery entityQuery : res.getResources()) {
            fillEntitiQueryVersionRelations(entityQuery, userDetails);
        }
        return res;
    }

    public EntityQuery getEntityQueryVersionById(String queryId, Integer versionId, UserDetails userDetails)
            throws LottabyteException {
        EntityQuery entityQuery = entityQueryRepository.getVersionById(queryId, versionId, userDetails);
        if (entityQuery == null)
            throw new LottabyteException(
                    Message.LBE00407,
                            userDetails.getLanguage(),
                    queryId);
        fillEntitiQueryVersionRelations(entityQuery, userDetails);
        return entityQuery;
    }

    private void fillEntitiQueryVersionRelations(EntityQuery entityQuery, UserDetails userDetails)
            throws LottabyteException {
        WorkflowableMetadata md = (WorkflowableMetadata) entityQuery.getMetadata();
        if (md.getAncestorDraftId() != null) {
            entityQuery.getMetadata().setTags(tagService.getArtifactTags(md.getAncestorDraftId(), userDetails));
        }
    }

    public SearchableEntityQuery getSearchableArtifact(EntityQuery entityQuery, UserDetails userDetails)
            throws LottabyteException {
        SearchableEntityQuery sa = SearchableEntityQuery.builder()
        .id(entityQuery.getMetadata().getId())
        .versionId(entityQuery.getMetadata().getVersionId())
        .name(entityQuery.getMetadata().getName())
        .description(entityQuery.getEntity().getDescription())
        .modifiedBy(entityQuery.getMetadata().getModifiedBy())
        .modifiedAt(entityQuery.getMetadata().getModifiedAt())
        .artifactType(entityQuery.getMetadata().getArtifactType())
        .effectiveStartDate(entityQuery.getMetadata().getEffectiveStartDate())
        .effectiveEndDate(entityQuery.getMetadata().getEffectiveEndDate())
        .tags(Helper.getEmptyListIfNull(entityQuery.getMetadata().getTags()).stream()
                .map(x -> x.getName()).collect(Collectors.toList()))

        .queryText(entityQuery.getEntity().getQueryText())
        .entityId(entityQuery.getEntity().getEntityId())
        .systemId(entityQuery.getEntity().getSystemId()).build();

        if (entityQuery.getEntity().getSystemId() != null && !entityQuery.getEntity().getSystemId().isEmpty())
            sa.setDomains(
                    entityQueryRepository.getDomainIdsBySystemId(entityQuery.getEntity().getSystemId(), userDetails));

        if (entityQuery.getEntity().getSystemId() != null && !entityQuery.getEntity().getSystemId().isEmpty()) {
            System sys = systemService.getSystemById(entityQuery.getEntity().getSystemId(), userDetails);
            if (sys != null)
                sa.setSystemName(sys.getName());
        }
        if (entityQuery.getEntity().getEntityId() != null && !entityQuery.getEntity().getEntityId().isEmpty()) {
            DataEntity ent = entityService.getEntityById(entityQuery.getEntity().getEntityId(), userDetails);
            if (ent != null)
                sa.setEntityName(ent.getName());
        }

        return sa;
    }

    @Override
    public String createDraft(String publishedId, WorkflowState workflowState, WorkflowType workflowType,
            UserDetails userDetails) throws LottabyteException {

        EntityQuery current = getEntityQueryById(publishedId, userDetails);

        ProcessInstance pi = null;
        String workflowTaskId = null;
        String draftId = UUID.randomUUID().toString();
        if (workflowService.isWorkflowEnabled(serviceArtifactType) && workflowService
                .getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

            pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.UPDATE,
                    userDetails);
            workflowTaskId = pi.getId();

        }
        entityQueryRepository.createDraftFromPublished(publishedId, draftId, workflowTaskId, userDetails);

        tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

        return draftId;
    }
}
