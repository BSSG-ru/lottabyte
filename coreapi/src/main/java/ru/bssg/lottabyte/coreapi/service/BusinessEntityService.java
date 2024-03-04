package ru.bssg.lottabyte.coreapi.service;

import org.flowable.engine.runtime.ProcessInstance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.businessEntity.*;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.reference.Reference;
import ru.bssg.lottabyte.core.model.reference.ReferenceEntity;
import ru.bssg.lottabyte.core.model.reference.ReferenceType;
import ru.bssg.lottabyte.core.model.reference.UpdatableReferenceEntity;
import ru.bssg.lottabyte.core.model.workflow.WorkflowTask;
import ru.bssg.lottabyte.core.model.workflow.WorkflowType;
import ru.bssg.lottabyte.core.ui.model.SearchColumn;
import ru.bssg.lottabyte.core.ui.model.SearchColumnForJoin;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.BusinessEntityRepository;
import ru.bssg.lottabyte.coreapi.repository.DomainRepository;
import ru.bssg.lottabyte.coreapi.util.Helper;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BusinessEntityService extends WorkflowableService<BusinessEntity> {
    private final ElasticsearchService elasticsearchService;
    private final BusinessEntityRepository businessEntityRepository;
    private final DomainRepository domainRepository;
    private final ReferenceService referenceService;
    private final CustomAttributeDefinitionService customAttributeDefinitionService;
    private final CommentService commentService;
    private final TagService tagService;
    private final WorkflowService workflowService;
    private final ArtifactType serviceArtifactType = ArtifactType.business_entity;

    private final SearchColumn[] searchableColumns = {
            new SearchColumn("id", SearchColumn.ColumnType.UUID),
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
            new SearchColumn("tech_name", SearchColumn.ColumnType.Text),
            new SearchColumn("definition", SearchColumn.ColumnType.Text),
            new SearchColumn("regulation", SearchColumn.ColumnType.Text),
            new SearchColumn("tags", SearchColumn.ColumnType.Text),
            new SearchColumn("alt_names", SearchColumn.ColumnType.Array),
            new SearchColumn("synonyms", SearchColumn.ColumnType.Text),
            new SearchColumn("be_links", SearchColumn.ColumnType.Text),
            new SearchColumn("domain_id", SearchColumn.ColumnType.UUID),
            new SearchColumn("domain.name", SearchColumn.ColumnType.Text),
            new SearchColumn("domain_name", SearchColumn.ColumnType.Text),
            new SearchColumn("parent_id", SearchColumn.ColumnType.UUID)
    };

    private final SearchColumnForJoin[] joinColumns = {
            new SearchColumnForJoin("domain_id", "system_to_domain", SearchColumn.ColumnType.UUID, "id", "system_id"),
            new SearchColumnForJoin("source_id", "reference", SearchColumn.ColumnType.UUID, "id", "target_id"),
            new SearchColumnForJoin("target_id", "reference", SearchColumn.ColumnType.UUID, "id", "source_id")
    };

    @Autowired
    public BusinessEntityService(ElasticsearchService elasticsearchService,
            BusinessEntityRepository businessEntityRepository,
            ReferenceService referenceService, CustomAttributeDefinitionService customAttributeDefinitionService,
            CommentService commentService, TagService tagService,
            WorkflowService workflowService, DomainRepository domainRepository) {
        super(businessEntityRepository, workflowService, tagService,
                ArtifactType.business_entity, elasticsearchService);
        this.elasticsearchService = elasticsearchService;
        this.businessEntityRepository = businessEntityRepository;
        this.referenceService = referenceService;
        this.customAttributeDefinitionService = customAttributeDefinitionService;
        this.commentService = commentService;
        this.tagService = tagService;
        this.workflowService = workflowService;
        this.domainRepository = domainRepository;
    }

    public BusinessEntity wfPublish(String businessEntityDraftId, UserDetails userDetails) throws LottabyteException {
        BusinessEntity draft = getBusinessEntityById(businessEntityDraftId, userDetails);
        String publishedId = ((WorkflowableMetadata) draft.getMetadata()).getPublishedId();
        if (draft == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, businessEntityDraftId);
        BusinessEntity businessEntity;

        if (publishedId == null) {
            publishedId = businessEntityRepository.publishDraft(businessEntityDraftId, null, userDetails);
        } else {
            businessEntityRepository.publishDraft(businessEntityDraftId, publishedId, userDetails);
        }
        if (referenceService.getReferenceBySourceId(publishedId, userDetails) != null)
            referenceService.deleteReferenceBySourceId(publishedId, userDetails);
        if (draft.getEntity().getSynonymIds() != null && !draft.getEntity().getSynonymIds().isEmpty()) {
            createSynonymsReference(draft.getEntity().getSynonymIds(), publishedId, publishedId, userDetails);
        }
        if (draft.getEntity().getBeLinkIds() != null && !draft.getEntity().getBeLinkIds().isEmpty()) {
            createBELinksReference(draft.getEntity().getBeLinkIds(), publishedId, publishedId, userDetails);
        }
        tagService.mergeTags(businessEntityDraftId, serviceArtifactType, publishedId, serviceArtifactType, userDetails);
        businessEntity = getBusinessEntityById(publishedId, userDetails);
        elasticsearchService.insertElasticSearchEntity(
                Collections.singletonList(getSearchableArtifact(businessEntity, userDetails)), userDetails);
        return businessEntity;
    }

    @Override
    public String createDraft(String businessEntityId, WorkflowState workflowState, WorkflowType workflowType, UserDetails userDetails) throws LottabyteException {

        BusinessEntity current = getBusinessEntityById(businessEntityId, userDetails);

        ProcessInstance pi = null;
        String workflowTaskId = null;
        String draftId = UUID.randomUUID().toString();
        if (workflowService.isWorkflowEnabled(serviceArtifactType) && workflowService
                .getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

            pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.UPDATE,
                    userDetails);
            workflowTaskId = pi.getId();

        }
        businessEntityRepository.createDraftFromPublished(businessEntityId, draftId, workflowTaskId, userDetails);

        if (current.getEntity().getSynonymIds() != null) {
            createSynonymsReference(current.getEntity().getSynonymIds(), draftId, businessEntityId,
                    userDetails);
        }

        if (current.getEntity().getBeLinkIds() != null) {
            createBELinksReference(current.getEntity().getBeLinkIds(), draftId, businessEntityId,
                    userDetails);
        }
        tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

        return draftId;
    }

    public void wfApproveRemoval(String draftBusinessEntityId, UserDetails userDetails) throws LottabyteException {
        BusinessEntity businessEntity = getBusinessEntityById(draftBusinessEntityId, userDetails);
        if (businessEntity == null)
            throw new LottabyteException(
                    Message.LBE03004, userDetails.getLanguage(),
                    serviceArtifactType, draftBusinessEntityId);
        String publishedId = ((WorkflowableMetadata) businessEntity.getMetadata()).getPublishedId();
        if (publishedId == null)
            throw new LottabyteException(
                    Message.LBE03006,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftBusinessEntityId);
        if (businessEntityRepository.hasChildren(publishedId, userDetails))
            throw new LottabyteException(Message.LBE02505, userDetails.getLanguage(), publishedId);

        businessEntityRepository.setStateById(businessEntity.getId(), ArtifactState.DRAFT_HISTORY,
                userDetails);
        businessEntityRepository.setStateById(publishedId, ArtifactState.REMOVED, userDetails);
        referenceService.deleteAllByArtifactId(publishedId, userDetails);
        elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(publishedId), userDetails);
    }

    public BusinessEntity getById(String id, UserDetails userDetails) throws LottabyteException {
        return getBusinessEntityById(id, userDetails);
    }

    public BusinessEntity getBusinessEntityById(String businessEntityId, UserDetails userDetails)
            throws LottabyteException {
        BusinessEntity businessEntity = businessEntityRepository.getById(businessEntityId, userDetails);
        if (businessEntity == null)
            throw new LottabyteException(Message.LBE02501,
                            userDetails.getLanguage(), businessEntityId);
        businessEntity.getMetadata().setTags(tagService.getArtifactTags(businessEntityId, userDetails));
        WorkflowableMetadata md = (WorkflowableMetadata) businessEntity.getMetadata();
        if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
            md.setDraftId(businessEntityRepository.getDraftId(md.getId(), userDetails));

        businessEntity.getEntity()
                .setSynonymIds(getSynonymsByBEId(businessEntityId, userDetails).stream().map(x -> x.getId())
                        .collect(Collectors.toList()));
        businessEntity.getEntity()
                .setBeLinkIds(getBELinksByBEId(businessEntityId, userDetails).stream().map(x -> x.getId())
                        .collect(Collectors.toList()));
        return businessEntity;
    }

    public BusinessEntity getBusinessEntityByIdAndStation(String businessEntityId, ArtifactState artifactState,
            UserDetails userDetails) throws LottabyteException {
        BusinessEntity businessEntity = businessEntityRepository.getByIdAndStation(businessEntityId, artifactState,
                userDetails);
        if (businessEntity == null)
            throw new LottabyteException(Message.LBE02501,
                            userDetails.getLanguage(), businessEntityId);
        businessEntity.getMetadata().setTags(tagService.getArtifactTags(businessEntityId, userDetails));
        WorkflowableMetadata md = (WorkflowableMetadata) businessEntity.getMetadata();
        if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
            md.setDraftId(businessEntityRepository.getDraftId(md.getId(), userDetails));

        businessEntity.getEntity()
                .setSynonymIds(getSynonymsByBEId(businessEntityId, userDetails).stream().map(x -> x.getId())
                        .collect(Collectors.toList()));
        businessEntity.getEntity()
                .setBeLinkIds(getBELinksByBEId(businessEntityId, userDetails).stream().map(x -> x.getId())
                        .collect(Collectors.toList()));
        return businessEntity;
    }

    public List<String> getBusinessEntityIdList(String businessEntityId, ArtifactType artifactType,
            UserDetails userDetails) {
        List<Reference> referenceForBusinessEntity = referenceService
                .getAllReferenceBySourceIdAndTargetType(businessEntityId, String.valueOf(artifactType), userDetails);
        return referenceForBusinessEntity.stream().map(r -> r.getEntity().getTargetId()).collect(Collectors.toList());
    }

    public PaginatedArtifactList<BusinessEntity> getBusinessEntitiesPaginated(Integer offset, Integer limit,
            String artifactState,
            UserDetails userDetails) throws LottabyteException {
        if (!EnumUtils.isValidEnum(ArtifactState.class, artifactState))
            throw new LottabyteException(
                    Message.LBE00067,
                            userDetails.getLanguage(),
                    artifactState);
        PaginatedArtifactList<BusinessEntity> res = businessEntityRepository.getAllPaginated(offset, limit,
                "/v1/business_entities",
                ArtifactState.valueOf(artifactState), userDetails);
        for (BusinessEntity be : res.getResources()) {
            be.getMetadata().setTags(tagService.getArtifactTags(be.getId(), userDetails));
            be.getEntity()
                    .setSynonymIds(getSynonymsByBEId(be.getId(), userDetails).stream().map(x -> x.getId())
                            .collect(Collectors.toList()));
            be.getEntity()
                    .setBeLinkIds(getBELinksByBEId(be.getId(), userDetails).stream().map(x -> x.getId())
                            .collect(Collectors.toList()));
        }
        return res;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public BusinessEntity createBusinessEntity(UpdatableBusinessEntityEntity newBusinessEntityEntity,
            UserDetails userDetails) throws LottabyteException {
        if (newBusinessEntityEntity.getName() == null || newBusinessEntityEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE02502,
                            userDetails.getLanguage(), newBusinessEntityEntity.getName());
        if (newBusinessEntityEntity.getSynonymIds() != null && !newBusinessEntityEntity.getSynonymIds().isEmpty()) {
            for (String synonymId : newBusinessEntityEntity.getSynonymIds()) {
                getBusinessEntityByIdAndStation(synonymId, ArtifactState.PUBLISHED, userDetails);
            }
        }

        String workflowTaskId = null;
        ProcessInstance pi = null;

        newBusinessEntityEntity.setId(UUID.randomUUID().toString());
        if (workflowService.isWorkflowEnabled(serviceArtifactType)
                && workflowService.getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

            pi = workflowService.startFlowableProcess(newBusinessEntityEntity.getId(), serviceArtifactType,
                    ArtifactAction.CREATE,
                    userDetails);
            workflowTaskId = pi.getId();

        }
        String newBusinessEntityId = businessEntityRepository.createBusinessEntity(newBusinessEntityEntity,
                workflowTaskId, userDetails);
        createSynonymsReference(newBusinessEntityEntity.getSynonymIds(), newBusinessEntityId, null, userDetails);
        createBELinksReference(newBusinessEntityEntity.getBeLinkIds(), newBusinessEntityId, null, userDetails);

        BusinessEntity businessEntity = getBusinessEntityById(newBusinessEntityId, userDetails);

        // elasticsearchService.insertElasticSearchEntity(Collections.singletonList(businessEntity.getSearchableArtifact()),
        // userDetails);
        return businessEntity;
    }

    public void createSynonymsReference(List<String> synonymIds, String newBusinessEntityId, String publishedId,
            UserDetails userDetails) throws LottabyteException {
        if (synonymIds != null && !synonymIds.isEmpty()) {
            Integer versionId = publishedId == null ? 0
                    : referenceService.getLastVersionByPublishedId(publishedId, userDetails);
            for (String targetId : synonymIds) {
                if (!targetId.equals(newBusinessEntityId)) {
                    ReferenceEntity referenceEntity = new ReferenceEntity();
                    referenceEntity.setSourceId(newBusinessEntityId);
                    referenceEntity.setSourceType(ArtifactType.business_entity);
                    referenceEntity.setPublishedId(publishedId);
                    referenceEntity.setVersionId(versionId);
                    referenceEntity.setTargetId(targetId);
                    referenceEntity.setTargetType(ArtifactType.business_entity);
                    referenceEntity.setReferenceType(ReferenceType.BUSINESS_ENTITY_TO_BUSINESS_ENTITY);

                    UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                    referenceService.createReference(newReferenceEntity, userDetails);
                }
            }
        }
    }

    public void createBELinksReference(List<String> beLinkIds, String newBusinessEntityId, String publishedId,
            UserDetails userDetails) throws LottabyteException {
        if (beLinkIds != null && !beLinkIds.isEmpty()) {
            Integer versionId = publishedId == null ? 0
                    : referenceService.getLastVersionByPublishedId(publishedId, userDetails);
            for (String targetId : beLinkIds) {
                if (!targetId.equals(newBusinessEntityId)) {
                    ReferenceEntity referenceEntity = new ReferenceEntity();
                    referenceEntity.setSourceId(newBusinessEntityId);
                    referenceEntity.setSourceType(ArtifactType.business_entity);
                    referenceEntity.setPublishedId(publishedId);
                    referenceEntity.setVersionId(versionId);
                    referenceEntity.setTargetId(targetId);
                    referenceEntity.setTargetType(ArtifactType.business_entity);
                    referenceEntity.setReferenceType(ReferenceType.BUSINESS_ENTITY_TO_BUSINESS_ENTITY_LINK);

                    UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                    referenceService.createReference(newReferenceEntity, userDetails);
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public BusinessEntity patchBusinessEntity(String businessEntityId,
            UpdatableBusinessEntityEntity businessEntityEntity, UserDetails userDetails) throws LottabyteException {
        if (businessEntityEntity.getSynonymIds() != null && !businessEntityEntity.getSynonymIds().isEmpty()
                && Objects.requireNonNull(businessEntityEntity.getSynonymIds()).contains(businessEntityId))
            throw new LottabyteException(Message.LBE02504,
                            userDetails.getLanguage(), businessEntityEntity.getName());
        BusinessEntity current = getBusinessEntityById(businessEntityId, userDetails);
        String draftId = null;
        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            draftId = businessEntityRepository.getDraftId(businessEntityId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE02503,
                                userDetails.getLanguage(),
                        draftId);
        }
        if (businessEntityEntity.getSynonymIds() != null && !businessEntityEntity.getSynonymIds().isEmpty()) {
            for (String synonymId : businessEntityEntity.getSynonymIds()) {
                getBusinessEntityByIdAndStation(synonymId, ArtifactState.PUBLISHED, userDetails);
            }
        }
        if (businessEntityEntity.getName() != null && businessEntityEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE02502,
                            userDetails.getLanguage(), businessEntityEntity.getName());
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
            businessEntityRepository.createDraftFromPublished(businessEntityId, draftId, workflowTaskId, userDetails);

            if (businessEntityEntity.getSynonymIds() != null) {
                createSynonymsReference(businessEntityEntity.getSynonymIds(), draftId, businessEntityId, userDetails);
            } else {
                if (current.getEntity().getSynonymIds() != null) {
                    createSynonymsReference(current.getEntity().getSynonymIds(), draftId, businessEntityId,
                            userDetails);
                }
            }
            if (businessEntityEntity.getBeLinkIds() != null) {
                createBELinksReference(businessEntityEntity.getBeLinkIds(), draftId, businessEntityId, userDetails);
            } else {
                if (current.getEntity().getBeLinkIds() != null) {
                    createBELinksReference(current.getEntity().getBeLinkIds(), draftId, businessEntityId,
                            userDetails);
                }
            }
            tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

        } else {
            draftId = businessEntityId;

            if (current.getEntity().getSynonymIds() != null && !current.getEntity().getSynonymIds().isEmpty()) {
                if (businessEntityEntity.getSynonymIds() != null) {
                    for (String synonymBusinessEntityId : current.getEntity().getSynonymIds()) {
                        referenceService.deleteByReferenceSourceIdAndTargetId(draftId, synonymBusinessEntityId,
                                userDetails);
                    }
                    createSynonymsReference(businessEntityEntity.getSynonymIds(), draftId, businessEntityId,
                            userDetails);
                }
            } else {
                if (businessEntityEntity.getSynonymIds() != null && !businessEntityEntity.getSynonymIds().isEmpty()) {
                    createSynonymsReference(businessEntityEntity.getSynonymIds(), draftId, businessEntityId,
                            userDetails);
                }
            }

            if (current.getEntity().getBeLinkIds() != null && !current.getEntity().getBeLinkIds().isEmpty()) {
                if (businessEntityEntity.getBeLinkIds() != null) {
                    for (String synonymBusinessEntityId : current.getEntity().getBeLinkIds()) {
                        referenceService.deleteByReferenceSourceIdAndTargetId(draftId, synonymBusinessEntityId,
                                userDetails);
                    }
                    createBELinksReference(businessEntityEntity.getBeLinkIds(), draftId, businessEntityId, userDetails);
                }
            } else {
                if (businessEntityEntity.getBeLinkIds() != null && !businessEntityEntity.getBeLinkIds().isEmpty()) {
                    createBELinksReference(businessEntityEntity.getBeLinkIds(), draftId, businessEntityId, userDetails);
                }
            }
        }

        businessEntityRepository.patchBusinessEntity(draftId, businessEntityEntity, userDetails);
        BusinessEntity businessEntity = getBusinessEntityById(draftId, userDetails);
        // elasticsearchService.updateElasticSearchEntity(Collections.singletonList(businessEntity.getSearchableArtifact()),
        // userDetails);
        return businessEntity;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public BusinessEntity deleteBusinessEntity(String businessEntityId, UserDetails userDetails)
            throws LottabyteException {
        BusinessEntity current = getBusinessEntityById(businessEntityId, userDetails);
        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {

            if (businessEntityRepository.hasChildren(businessEntityId, userDetails)) {
                throw new LottabyteException(Message.LBE02505,
                        userDetails.getLanguage(), businessEntityId);
            }

            String draftId = businessEntityRepository.getDraftId(businessEntityId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE00505,
                                userDetails.getLanguage(),
                        draftId);

            ProcessInstance pi = null;
            draftId = null;
            String workflowTaskId = null;

            draftId = UUID.randomUUID().toString();
            pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.REMOVE, userDetails);
            workflowTaskId = pi.getId();

            businessEntityRepository.createDraftFromPublished(current.getId(), draftId, workflowTaskId, userDetails);
            createSynonymsReference(current.getEntity().getSynonymIds(), draftId, businessEntityId, userDetails);
            createBELinksReference(current.getEntity().getBeLinkIds(), draftId, businessEntityId, userDetails);
            return getBusinessEntityById(draftId, userDetails);
        } else {
            referenceService.deleteReferenceBySourceId(businessEntityId, userDetails);
            tagService.deleteAllTagsByArtifactId(businessEntityId, userDetails);
            businessEntityRepository.deleteById(businessEntityId, userDetails);
            return null;
        }

        /*
         * customAttributeDefinitionService.deleteAllCustomAttributesByArtifactId(
         * businessEntityId, userDetails);
         * tagService.deleteAllTagsByArtifactId(businessEntityId, userDetails);
         * commentService.deleteAllCommentsByArtifactId(businessEntityId, userDetails);
         * businessEntityRepository.deleteById(businessEntityId, userDetails);
         * ArchiveResponse archiveResponse = new ArchiveResponse();
         * archiveResponse.setArchivedGuids(Collections.singletonList(businessEntityId))
         * ;
         * elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(
         * businessEntityId), userDetails);
         * return archiveResponse;
         */
    }

    public List<BusinessEntity> getSynonymsByBEId(String beId, UserDetails userDetails) {
        return businessEntityRepository.getSynonymsByBEId(beId, userDetails);
    }

    public List<BusinessEntity> getBELinksByBEId(String beId, UserDetails userDetails) {
        return businessEntityRepository.getBELinksByBEId(beId, userDetails);
    }

    public List<BusinessEntityTreeNode> getBETree(SearchRequestWithJoin request, UserDetails userDetails)
            throws LottabyteException {
        // SearchRequestWithJoin request = new SearchRequestWithJoin();
        request.setOffset(0);
        request.setLimit(10000);
        // request.setFilters(new ArrayList<>());
        // request.setFiltersForJoin(new ArrayList<>());
        // request.setGlobalQuery("");
        SearchResponse<FlatBusinessEntity> res = searchBusinessEntity(request, userDetails);

        List<BusinessEntityTreeNode> list = new ArrayList<>();

        Map<String, BusinessEntityTreeNode> addedNodes = new HashMap<>();
        List<String> allBEIds = new ArrayList<>();
        for (FlatBusinessEntity be : res.getItems())
            allBEIds.add(be.getId());

        while (addedNodes.size() < res.getItems().size()) {
            for (FlatBusinessEntity be : res.getItems()) {

                if (!addedNodes.containsKey(be.getId()) && (be.getParentId() == null || be.getParentId().isEmpty()
                        || addedNodes.containsKey(be.getParentId()) || !allBEIds.contains(be.getParentId()))) {
                    BusinessEntityTreeNode node = new BusinessEntityTreeNode();
                    node.setKey(be.getId());
                    BusinessEntityTreeNodeData data = new BusinessEntityTreeNodeData();
                    data.setId(be.getId());
                    data.setName(be.getName());
                    data.setTechName(be.getTechName());
                    data.setDomainName(be.getDomainName());
                    data.setAltNames(StringUtils.join(be.getAltNames(), ", "));
                    data.setSynonyms(StringUtils
                            .join(be.getSynonyms().stream().map(x -> x.getName()).collect(Collectors.toList()), ", "));
                    data.setBeLinks(StringUtils
                            .join(be.getBeLinks().stream().map(x -> x.getName()).collect(Collectors.toList()), ", "));
                    data.setModified(be.getModified().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                    data.setWorkflowState(be.getWorkflowState());
                    data.setTags(StringUtils.join(be.getTags(), ", "));
                    node.setData(data);
                    node.setChildren(new ArrayList<BusinessEntityTreeNode>());

                    if (be.getParentId() != null && !be.getParentId().isEmpty()) {
                        BusinessEntityTreeNode p = addedNodes.get(be.getParentId());
                        if (p != null)
                            p.getChildren().add(node);
                        else
                            list.add(node);
                    } else
                        list.add(node);

                    addedNodes.put(be.getId(), node);
                }
            }
        }

        return list;
    }

    public SearchResponse<FlatBusinessEntity> searchBusinessEntity(SearchRequestWithJoin request,
            UserDetails userDetails) throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        SearchResponse<FlatBusinessEntity> res = businessEntityRepository.searchBusinessEntity(request,
                searchableColumns, joinColumns, userDetails);
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
        for (FlatBusinessEntity item : res.getItems()) {
            item.setSynonyms(getSynonymsByBEId(item.getId(), userDetails).stream().map(y -> FlatRelation.builder()
                    .id(y.getId()).name(y.getName()).build()).collect(Collectors.toList()));
            item.setBeLinks(getBELinksByBEId(item.getId(), userDetails).stream().map(y -> FlatRelation.builder()
                    .id(y.getId()).name(y.getName()).build()).collect(Collectors.toList()));
        }
        return res;
    }

    public PaginatedArtifactList<BusinessEntity> getBusinessEntityVersions(String businessEntityId, Integer offset,
            Integer limit, UserDetails userDetails) throws LottabyteException {
        if (!businessEntityRepository.existsById(businessEntityId,
                new ArtifactState[] { ArtifactState.PUBLISHED, ArtifactState.REMOVED }, userDetails)) {
            // throw new LottabyteException(HttpStatus.NOT_FOUND,
            // Message.format(Message.LBE00503, assetId));
            PaginatedArtifactList<BusinessEntity> res = new PaginatedArtifactList<>();
            res.setCount(0);
            res.setOffset(offset);
            res.setLimit(limit);
            res.setResources(new ArrayList<>());
            return res;
        }
        PaginatedArtifactList<BusinessEntity> res = businessEntityRepository.getVersionsById(businessEntityId, offset,
                limit, "/v1/business_entities", userDetails);
        for (BusinessEntity be : res.getResources()) {
            fillBusinessEntityVersionRelations(be, userDetails);
        }
        return res;
    }

    public BusinessEntity getBusinessEntityVersionById(String businessEntityId, Integer versionId,
            UserDetails userDetails) throws LottabyteException {
        BusinessEntity be = businessEntityRepository.getVersionById(businessEntityId, versionId, userDetails);
        if (be == null)
            throw new LottabyteException(
                    Message.LBE02501,
                            userDetails.getLanguage(),
                    businessEntityId);
        fillBusinessEntityVersionRelations(be, userDetails);
        return be;
    }

    private void fillBusinessEntityVersionRelations(BusinessEntity be, UserDetails userDetails) {
        WorkflowableMetadata md = (WorkflowableMetadata) be.getMetadata();
        if (md.getAncestorDraftId() != null) {
            be.getMetadata().setTags(tagService.getArtifactTags(md.getAncestorDraftId(), userDetails));
            be.getEntity().setSynonymIds(
                    getSynonymsByBEId(md.getAncestorDraftId(), userDetails).stream().map(x -> x.getId())
                            .collect(Collectors.toList()));
            be.getEntity().setBeLinkIds(
                    getBELinksByBEId(md.getAncestorDraftId(), userDetails).stream().map(x -> x.getId())
                            .collect(Collectors.toList()));
        } else {
            be.getEntity()
                    .setSynonymIds(getSynonymsByBEId(be.getId(), userDetails).stream().map(x -> x.getId())
                            .collect(Collectors.toList()));
            be.getEntity()
                    .setBeLinkIds(getBELinksByBEId(be.getId(), userDetails).stream().map(x -> x.getId())
                            .collect(Collectors.toList()));
        }
    }

    public SearchableBusinessEntity getSearchableArtifact(BusinessEntity businessEntity, UserDetails userDetails) {
        SearchableBusinessEntity searchableBusinessEntity = SearchableBusinessEntity.builder()
            .id(businessEntity.getMetadata().getId())
            .versionId(businessEntity.getMetadata().getVersionId())
            .name(businessEntity.getMetadata().getName())
            .description(businessEntity.getEntity().getDescription())
            .modifiedBy(businessEntity.getMetadata().getModifiedBy())
            .modifiedAt(businessEntity.getMetadata().getModifiedAt())
            .artifactType(businessEntity.getMetadata().getArtifactType())
            .effectiveStartDate(businessEntity.getMetadata().getEffectiveStartDate())
            .effectiveEndDate(businessEntity.getMetadata().getEffectiveEndDate())
            .tags(Helper.getEmptyListIfNull(businessEntity.getMetadata().getTags()).stream()
                .map(x -> x.getName()).collect(Collectors.toList()))

            .altNames(businessEntity.getEntity().getAltNames())
            .techName(businessEntity.getEntity().getTechName())
            .definition(businessEntity.getEntity().getDefinition())
            .regulation(businessEntity.getEntity().getRegulation())
            .synonymIds(businessEntity.getEntity().getSynonymIds())
            .beLinkIds(businessEntity.getEntity().getBeLinkIds())

            .domainId(businessEntity.getEntity().getDomainId())
            .formula(businessEntity.getEntity().getFormula())
            .examples(businessEntity.getEntity().getExamples())
            .link(businessEntity.getEntity().getLink())
            .datatypeId(businessEntity.getEntity().getDatatypeId())
            .limits(businessEntity.getEntity().getLimits())
            .roles(businessEntity.getEntity().getRoles()).build();

        String dId = businessEntity.getEntity().getDomainId();

        Domain d = (dId != null && !dId.isEmpty())
                ? domainRepository.getById(businessEntity.getEntity().getDomainId(), userDetails)
                : null;
        if (d != null)
            searchableBusinessEntity.setDomainName(d.getName());

        searchableBusinessEntity.setDomains(Collections.singletonList(businessEntity.getEntity().getDomainId()));

        return searchableBusinessEntity;
    }
}
