package ru.bssg.lottabyte.coreapi.service;

import org.flowable.engine.runtime.ProcessInstance;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.businessEntity.BusinessEntity;
import ru.bssg.lottabyte.core.model.dataentity.*;
import ru.bssg.lottabyte.core.model.datatype.DataType;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRule;
import ru.bssg.lottabyte.core.model.entitySample.UpdatableEntitySampleDQRule;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.indicator.FlatIndicator;
import ru.bssg.lottabyte.core.model.indicator.Indicator;
import ru.bssg.lottabyte.core.model.indicator.SearchableIndicator;
import ru.bssg.lottabyte.core.model.indicator.UpdatableIndicatorEntity;
import ru.bssg.lottabyte.core.model.reference.Reference;
import ru.bssg.lottabyte.core.model.reference.ReferenceEntity;
import ru.bssg.lottabyte.core.model.reference.ReferenceType;
import ru.bssg.lottabyte.core.model.reference.UpdatableReferenceEntity;
import ru.bssg.lottabyte.core.model.relation.Relation;
import ru.bssg.lottabyte.core.model.workflow.WorkflowTask;
import ru.bssg.lottabyte.core.model.workflow.WorkflowType;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.DataTypeRepository;
import ru.bssg.lottabyte.coreapi.repository.DomainRepository;
import ru.bssg.lottabyte.coreapi.repository.EntitySampleRepository;
import ru.bssg.lottabyte.coreapi.repository.IndicatorRepository;
import ru.bssg.lottabyte.coreapi.util.Helper;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IndicatorService extends WorkflowableService<Indicator> {
    private final DataAssetService dataAssetService;
    private final CustomAttributeDefinitionService customAttributeDefinitionService;
    private final CommentService commentService;
    private final ElasticsearchService elasticsearchService;
    private final IndicatorRepository indicatorRepository;
    private final DomainRepository domainRepository;
    private final TagService tagService;
    private final WorkflowService workflowService;
    private final ReferenceService referenceService;
    private final ArtifactType serviceArtifactType = ArtifactType.indicator;
    private final EntitySampleRepository entitySampleRepository;
    private final DataTypeRepository dataTypeRepository;

    private final SearchColumn[] searchableColumns = {
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
            new SearchColumn("calc_code", SearchColumn.ColumnType.Text),
            new SearchColumn("tags", SearchColumn.ColumnType.Text),
            new SearchColumn("domain.name", SearchColumn.ColumnType.Text),
            new SearchColumn("indicator_type.name", SearchColumn.ColumnType.Text),
            new SearchColumn("version_id", SearchColumn.ColumnType.Number),
            new SearchColumn("dq_checks", SearchColumn.ColumnType.Array)
    };

    private final SearchColumnForJoin[] joinColumns = {
    };

    @Autowired
    public IndicatorService(DataAssetService dataAssetService,
            CustomAttributeDefinitionService customAttributeDefinitionService,
            CommentService commentService, ElasticsearchService elasticsearchService,
            IndicatorRepository indicatorRepository, DomainRepository domainRepository,
            EntitySampleRepository entitySampleRepository, TagService tagService,
            WorkflowService workflowService, ReferenceService referenceService,
            DataTypeRepository dataTypeRepository) {
        super(indicatorRepository, workflowService, tagService, ArtifactType.indicator, elasticsearchService);
        this.dataAssetService = dataAssetService;
        this.customAttributeDefinitionService = customAttributeDefinitionService;
        this.commentService = commentService;
        this.elasticsearchService = elasticsearchService;
        this.indicatorRepository = indicatorRepository;
        this.domainRepository = domainRepository;
        this.tagService = tagService;
        this.workflowService = workflowService;
        this.referenceService = referenceService;
        this.entitySampleRepository = entitySampleRepository;
        this.dataTypeRepository = dataTypeRepository;
    }

    public Boolean allIndicatorsExist(List<String> systemIds, UserDetails userDetails) {
        return indicatorRepository.allIndicatorsExist(systemIds, userDetails);
    }

    public Indicator wfPublish(String draftIndicatorId, UserDetails userDetails) throws LottabyteException {
        Indicator draft = getIndicatorById(draftIndicatorId, userDetails);
        String publishedId = ((WorkflowableMetadata) draft.getMetadata()).getPublishedId();
        if (draft == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftIndicatorId);
        Indicator indicator;

        if (publishedId == null) {
            publishedId = indicatorRepository.publishDraft(draftIndicatorId, null, userDetails);

            if (draft.getEntity() != null) {
                updateReferenceForAssets(draft.getEntity().getDataAssetIds(), draft.getId(), publishedId, userDetails);
                createReferenceForAssets(draft.getEntity().getDataAssetIds(), publishedId, publishedId, userDetails);
            }

            if (draft.getEntity().getDqRules() != null && !draft.getEntity().getDqRules().isEmpty())
                for (EntitySampleDQRule s : draft.getEntity().getDqRules())
                    indicatorRepository.addDQRule(publishedId, s, userDetails);

            tagService.mergeTags(draftIndicatorId, serviceArtifactType, publishedId, serviceArtifactType, userDetails);
            indicator = getIndicatorById(publishedId, userDetails);
            elasticsearchService.insertElasticSearchEntity(
                    Collections.singletonList(getSearchableArtifact(indicator, userDetails)), userDetails);
        } else {
            indicatorRepository.publishDraft(draftIndicatorId, publishedId, userDetails);

            Indicator currentPublished = getIndicatorById(publishedId, userDetails);
            updateDQRules(publishedId, draft.getEntity().getDqRules(), currentPublished.getEntity().getDqRules(),
                    userDetails);
            if (referenceService.getReferenceBySourceId(publishedId, userDetails) != null)
                referenceService.deleteReferenceBySourceId(publishedId, userDetails);

            if (draft.getEntity() != null) {
                updateReferenceForAssets(draft.getEntity().getDataAssetIds(), draft.getId(), publishedId, userDetails);
                log.info("createReferenceForAssets " + draft.getEntity().getDataAssetIds().size() + " " + publishedId);
                createReferenceForAssets(draft.getEntity().getDataAssetIds(), publishedId, publishedId, userDetails);
            }

            if (draft.getEntity().getTermLinkIds() != null && !draft.getEntity().getTermLinkIds().isEmpty()) {
                createTermLinksReference(draft.getEntity().getTermLinkIds(), publishedId, publishedId, userDetails);
            }

            tagService.mergeTags(draftIndicatorId, serviceArtifactType, publishedId, serviceArtifactType, userDetails);

            indicator = getIndicatorById(publishedId, userDetails);
            elasticsearchService.updateElasticSearchEntity(
                    Collections.singletonList(getSearchableArtifact(indicator, userDetails)),
                    userDetails);
        }

        return indicator;
    }

    public void wfApproveRemoval(String draftSystemId, UserDetails userDetails) throws LottabyteException {
        Indicator indicator = getIndicatorById(draftSystemId, userDetails);
        if (indicator == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftSystemId);
        String publishedId = ((WorkflowableMetadata) indicator.getMetadata()).getPublishedId();
        if (publishedId == null)
            throw new LottabyteException(
                    Message.LBE03006,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftSystemId);
        indicatorRepository.setStateById(indicator.getId(), ArtifactState.DRAFT_HISTORY, userDetails);
        indicatorRepository.setStateById(publishedId, ArtifactState.REMOVED, userDetails);
        elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(publishedId), userDetails);
    }

    public Indicator getById(String id, UserDetails userDetails) throws LottabyteException {
        return getIndicatorById(id, userDetails);
    }

    public List<String> entityAttributeExistInAllFormulas(String entityAttributeId, UserDetails userDetails) {
        return indicatorRepository.entityAttributeExistInAllFormulas(entityAttributeId, userDetails);
    }

    public List<String> indicatorExistInAllFormulas(String indicatorId, UserDetails userDetails) {
        return indicatorRepository.indicatorExistInAllFormulas(indicatorId, userDetails);
    }

    public Indicator getIndicatorById(String indicatorId, UserDetails userDetails) throws LottabyteException {
        Indicator indicator = indicatorRepository.getById(indicatorId, userDetails);
        if (indicator == null)
            throw new LottabyteException(Message.LBE02401,
                            userDetails.getLanguage(), indicatorId);

        List<EntitySampleDQRule> dqRules = entitySampleRepository.getSampleDQRulesByIndicator(indicatorId, userDetails);
        indicator.getEntity().setDqRules(dqRules);

        List<Reference> referenceForDataAsset = referenceService.getAllReferenceBySourceIdAndTargetType(indicatorId,
                String.valueOf(ArtifactType.data_asset), userDetails);
        List<String> dataAssetIdList = referenceForDataAsset.stream().map(r -> r.getEntity().getTargetId())
                .collect(Collectors.toList());
        indicator.getEntity().setDataAssetIds(dataAssetIdList);

        indicator.getEntity().setTermLinkIds(
                getTermLinksById(indicatorId, userDetails).stream().map(x -> x.getId()).collect(Collectors.toList()));

        indicator.getMetadata().setTags(tagService.getArtifactTags(indicatorId, userDetails));
        WorkflowableMetadata md = (WorkflowableMetadata) indicator.getMetadata();
        if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
            md.setDraftId(indicatorRepository.getDraftId(md.getId(), userDetails));
        return indicator;
    }

    public Map<String, List<DataEntityAttributeEntity>> getEntityAttributesByIndicatorId(String indicatorId,
            UserDetails userDetails) throws LottabyteException {
        getIndicatorById(indicatorId, userDetails);
        return indicatorRepository.getEntityAttributesByIndicatorId(indicatorId, userDetails);
    }

    public PaginatedArtifactList<Indicator> getIndicatorsPaginated(Integer offset, Integer limit, String artifactState,
            UserDetails userDetails) throws LottabyteException {
        if (!EnumUtils.isValidEnum(ArtifactState.class, artifactState))
            throw new LottabyteException(
                    Message.LBE00067,
                            userDetails.getLanguage(),
                    artifactState);
        PaginatedArtifactList<Indicator> res = indicatorRepository.getAllPaginated(offset, limit, "/v1/indicators",
                ArtifactState.valueOf(artifactState), userDetails);
        for (Indicator ind : res.getResources()) {
            ind.getMetadata().setTags(tagService.getArtifactTags(ind.getId(), userDetails));
        }
        return res;
    }

    @Transactional
    public Indicator createIndicator(UpdatableIndicatorEntity newIndicatorEntity, UserDetails userDetails)
            throws LottabyteException {
        if (newIndicatorEntity.getName() == null || newIndicatorEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE02402,
                            userDetails.getLanguage(), newIndicatorEntity.getName());
        if (newIndicatorEntity.getIndicatorTypeId() == null || newIndicatorEntity.getIndicatorTypeId().isEmpty())
            throw new LottabyteException(Message.LBE02404,
                            userDetails.getLanguage(), newIndicatorEntity.getName());
        if (indicatorRepository.getIndicatorTypeById(newIndicatorEntity.getIndicatorTypeId(), userDetails) == null)
            throw new LottabyteException(Message.LBE02404,
                            userDetails.getLanguage(), newIndicatorEntity.getName());

        if (newIndicatorEntity.getFormula() != null) {
            try {
                validateFormula(newIndicatorEntity.getDataAssetIds(), newIndicatorEntity.getName(),
                        newIndicatorEntity.getFormula(), userDetails);
            } catch (JsonProcessingException e) {
                throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage());
            }
        }

        String workflowTaskId = null;
        ProcessInstance pi = null;
        newIndicatorEntity.setId(UUID.randomUUID().toString());
        if (workflowService.isWorkflowEnabled(serviceArtifactType)
                && workflowService.getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

            pi = workflowService.startFlowableProcess(newIndicatorEntity.getId(), serviceArtifactType,
                    ArtifactAction.CREATE,
                    userDetails);
            workflowTaskId = pi.getId();

        }
        String newIndicatorId = indicatorRepository.createIndicator(newIndicatorEntity, workflowTaskId, userDetails);

        createReferenceForAssets(newIndicatorEntity.getDataAssetIds(), newIndicatorId, null, userDetails);
        createTermLinksReference(newIndicatorEntity.getTermLinkIds(), newIndicatorId, null, userDetails);

        Indicator indicator = getIndicatorById(newIndicatorId, userDetails);

        // elasticsearchService.insertElasticSearchEntity(Collections.singletonList(indicator.getSearchableArtifact()),
        // userDetails);
        return indicator;
    }

    public void createTermLinksReference(List<String> termLinkIds, String newIndicatorId, String publishedId,
            UserDetails userDetails) throws LottabyteException {
        if (termLinkIds != null && !termLinkIds.isEmpty()) {
            Integer versionId = publishedId == null ? 0
                    : referenceService.getLastVersionByPublishedId(publishedId, userDetails);
            for (String targetId : termLinkIds) {
                if (!targetId.equals(newIndicatorId)) {
                    ReferenceEntity referenceEntity = new ReferenceEntity();
                    referenceEntity.setSourceId(newIndicatorId);
                    referenceEntity.setSourceType(ArtifactType.indicator);
                    referenceEntity.setPublishedId(publishedId);
                    referenceEntity.setVersionId(versionId);
                    referenceEntity.setTargetId(targetId);
                    referenceEntity.setTargetType(ArtifactType.business_entity);
                    referenceEntity.setReferenceType(ReferenceType.INDICATOR_TO_BUSINESS_ENTITY_LINK);

                    UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                    referenceService.createReference(newReferenceEntity, userDetails);
                }
            }
        }
    }

    public void updateReferenceForAssets(List<String> dataAssetIds, String newIndicatorId, String publishedId,
            UserDetails userDetails) throws LottabyteException {
        if (dataAssetIds != null && !dataAssetIds.isEmpty()) {
            for (String targetId : dataAssetIds) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newIndicatorId);
                referenceEntity.setTargetId(targetId);
                referenceEntity.setPublishedId(publishedId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.patchReferenceBySourceIdAndTargetId(newReferenceEntity, userDetails);
            }
        }
    }

    public void createReferenceForAssets(List<String> dataAssetIds, String newIndicatorId, String publishedId,
            UserDetails userDetails) throws LottabyteException {
        Integer versionId = referenceService.getLastVersionByPublishedId(publishedId, userDetails);

        if (dataAssetIds != null && !dataAssetIds.isEmpty()) {
            for (String targetId : dataAssetIds) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newIndicatorId);
                referenceEntity.setSourceType(ArtifactType.indicator);
                referenceEntity.setTargetId(targetId);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(ArtifactType.data_asset);
                referenceEntity.setReferenceType(ReferenceType.INDICATOR_TO_DATA_ASSET);
                referenceEntity.setVersionId(versionId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.createReference(newReferenceEntity, userDetails);
            }
        }
    }

    public Indicator patchIndicator(String indicatorId, UpdatableIndicatorEntity indicatorEntity,
            UserDetails userDetails) throws LottabyteException {
        Indicator current = getIndicatorById(indicatorId, userDetails);
        String draftId = null;

        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            draftId = indicatorRepository.getDraftId(indicatorId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE02403,
                                userDetails.getLanguage(),
                        draftId);
        }
        try {
            validateFormula(
                    indicatorEntity.getDataAssetIds() == null ? current.getEntity().getDataAssetIds()
                            : indicatorEntity.getDataAssetIds(),
                    current.getName(), indicatorEntity.getFormula() == null ? current.getEntity().getFormula()
                            : indicatorEntity.getFormula(),
                    userDetails);
        } catch (JsonProcessingException e) {
            throw new LottabyteException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        if (indicatorEntity.getDataAssetIds() != null && !indicatorEntity.getDataAssetIds().isEmpty()
                && !dataAssetService.allDataAssetsExist(indicatorEntity.getDataAssetIds(), userDetails))
            throw new LottabyteException(Message.LBE03105,
                            userDetails.getLanguage(),
                            org.apache.commons.lang3.StringUtils.join(indicatorEntity.getDataAssetIds(), ", "));
        if (indicatorEntity.getName() != null && indicatorEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE02402,
                            userDetails.getLanguage(), indicatorEntity.getName());

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
            indicatorRepository.createDraftFromPublished(indicatorId, draftId, workflowTaskId, userDetails);

            if (indicatorEntity.getDqRules() != null && !indicatorEntity.getDqRules().isEmpty()) {
                for (EntitySampleDQRule s : indicatorEntity.getDqRules()) {
                    indicatorRepository.addDQRule(draftId, s, userDetails);
                }
            } else if (current.getEntity().getDqRules() != null && !current.getEntity().getDqRules().isEmpty()) {
                for (EntitySampleDQRule s : current.getEntity().getDqRules()) {
                    indicatorRepository.addDQRule(draftId, s, userDetails);
                }
            }

            if (indicatorEntity.getDataAssetIds() != null)
                createReferenceForAssets(indicatorEntity.getDataAssetIds(), draftId, indicatorId, userDetails);
            else {
                if (current.getEntity().getDataAssetIds() != null && !current.getEntity().getDataAssetIds().isEmpty())
                    createReferenceForAssets(current.getEntity().getDataAssetIds(), draftId, indicatorId, userDetails);
            }

            if (indicatorEntity.getTermLinkIds() != null) {
                createTermLinksReference(indicatorEntity.getTermLinkIds(), draftId, indicatorId, userDetails);
            } else {
                if (current.getEntity().getTermLinkIds() != null) {
                    createTermLinksReference(current.getEntity().getTermLinkIds(), draftId, indicatorId,
                            userDetails);
                }
            }

            tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

        } else {
            draftId = indicatorId;
            updateDQRules(indicatorId, indicatorEntity.getDqRules(), current.getEntity().getDqRules(), userDetails);
            if (indicatorEntity.getDataAssetIds() != null) {
                if (current.getEntity().getDataAssetIds() != null && !current.getEntity().getDataAssetIds().isEmpty()) {
                    for (String id : current.getEntity().getDataAssetIds()) {
                        referenceService.deleteByReferenceSourceIdAndTargetId(draftId, id, userDetails);
                    }
                }
                createReferenceForAssets(indicatorEntity.getDataAssetIds(), draftId, indicatorId, userDetails);
            }

            if (current.getEntity().getTermLinkIds() != null && !current.getEntity().getTermLinkIds().isEmpty()) {
                if (indicatorEntity.getTermLinkIds() != null) {
                    for (String id : current.getEntity().getTermLinkIds()) {
                        referenceService.deleteByReferenceSourceIdAndTargetId(draftId, id,
                                userDetails);
                    }
                    createTermLinksReference(indicatorEntity.getTermLinkIds(), draftId, indicatorId, userDetails);
                }
            } else {
                if (indicatorEntity.getTermLinkIds() != null && !indicatorEntity.getTermLinkIds().isEmpty()) {
                    createTermLinksReference(indicatorEntity.getTermLinkIds(), draftId, indicatorId, userDetails);
                }
            }
        }

        indicatorRepository.patchIndicator(draftId, indicatorEntity, userDetails);
        return getIndicatorById(draftId, userDetails);
    }

    private void updateDQRules(String indicatorId, List<EntitySampleDQRule> ids, List<EntitySampleDQRule> currentIds,
            UserDetails userDetails) {
        if (currentIds != null && ids != null) {
            ids.stream().filter(x -> !EntitySampleService.containsDQRule(currentIds, x)).collect(Collectors.toList())
                    .forEach(y -> indicatorRepository.addDQRule(indicatorId, y, userDetails));
            currentIds.stream().filter(x -> !EntitySampleService.containsDQRule(ids, x)).collect(Collectors.toList())
                    .forEach(y -> indicatorRepository.removeDQRule(y.getId(), userDetails));
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Indicator deleteIndicator(String indicatorId, UserDetails userDetails) throws LottabyteException {
        Indicator current = getIndicatorById(indicatorId, userDetails);
        List<String> indicatorIdList = indicatorExistInAllFormulas(indicatorId, userDetails);
        for (String id : indicatorIdList) {
            if (!id.equals(indicatorId))
                throw new LottabyteException(Message.LBE03204,
                                userDetails.getLanguage(), indicatorId);
        }

        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            String draftId = indicatorRepository.getDraftId(indicatorId, userDetails);
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

            indicatorRepository.createDraftFromPublished(current.getId(), draftId, workflowTaskId, userDetails);

            createTermLinksReference(current.getEntity().getTermLinkIds(), draftId, indicatorId, userDetails);
            return getIndicatorById(draftId, userDetails);
        } else {
            referenceService.deleteReferenceBySourceId(indicatorId, userDetails);
            tagService.deleteAllTagsByArtifactId(indicatorId, userDetails);
            indicatorRepository.deleteById(indicatorId, userDetails);
            return null;
        }
    }

    private void validateFormula(List<String> indicatorDataAssetIds, String indicatorName, String formula,
            UserDetails userDetails) throws LottabyteException, JsonProcessingException {
        if (formula == null || formula.isEmpty())
            return;

        HashMap<String, String> failedEntityMap = new HashMap<>();
        JSONObject obj = new JSONObject(formula);
        Map<String, Object> entityMap = new ObjectMapper().readValue(obj.getJSONObject("entityMap").toString(),
                HashMap.class);
        for (Object value : entityMap.values()) {
            Map<String, Object> mention = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) value)
                    .get("data")).get("mention");
            String id = mention.get("id").toString();
            String artifactType = mention.get("artifact_type").toString();
            if (artifactType.equals("entity_attribute")) {
                if (!indicatorRepository.entityAttributeExistsInDataAssets(id, indicatorDataAssetIds, userDetails))
                    failedEntityMap.put(id, artifactType);
            } else {
                if (!indicatorRepository.existArtifactById(id, ArtifactType.fromString(artifactType), userDetails)) {
                    failedEntityMap.put(id, artifactType);
                }
            }
        }
        if (!failedEntityMap.isEmpty())
            throw new LottabyteException(Message.LBE03203,
                            userDetails.getLanguage(), failedEntityMap, indicatorName);
    }

    public List<BusinessEntity> getTermLinksById(String beId, UserDetails userDetails) {
        return indicatorRepository.getTermLinksById(beId, userDetails);
    }

    private void validateFormulaForDelete(String indicatorId, String formula, UserDetails userDetails)
            throws LottabyteException, JsonProcessingException {
        if (StringUtils.isNullOrEmpty(formula))
            return;
        JSONObject obj = new JSONObject(formula);
        Map<String, Object> entityMap = new ObjectMapper().readValue(obj.getJSONObject("entityMap").toString(),
                HashMap.class);
        for (Object value : entityMap.values()) {
            Map<String, Object> f = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) value)
                    .get("data")).get("mention");
            String id = f.get("id").toString();
            String artifactType = f.get("artifact_type").toString();
            if (indicatorId.equals(id) && ArtifactType.fromString(artifactType) == ArtifactType.indicator)
                throw new LottabyteException(Message.LBE03204,
                                userDetails.getLanguage(), formula);
        }
    }

    public SearchResponse<FlatIndicator> searchIndicators(SearchRequestWithJoin request, UserDetails userDetails)
            throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        SearchResponse<FlatIndicator> res = indicatorRepository.searchIndicators(request, searchableColumns,
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

    public PaginatedArtifactList<Indicator> getIndicatorVersions(String indicatorId, Integer offset, Integer limit,
            UserDetails userDetails) throws LottabyteException {
        if (!indicatorRepository.existsById(indicatorId,
                new ArtifactState[] { ArtifactState.PUBLISHED, ArtifactState.REMOVED }, userDetails)) {
            // throw new LottabyteException(HttpStatus.NOT_FOUND,
            // Message.format(Message.LBE00503, assetId));
            PaginatedArtifactList<Indicator> res = new PaginatedArtifactList<>();
            res.setCount(0);
            res.setOffset(offset);
            res.setLimit(limit);
            res.setResources(new ArrayList<>());
            return res;
        }
        PaginatedArtifactList<Indicator> res = indicatorRepository.getVersionsById(indicatorId, offset, limit,
                "/v1/indicators/", userDetails);
        for (Indicator indicator : res.getResources()) {
            fillIndicatorVersionRelations(indicator, userDetails);
        }
        return res;
    }

    public Indicator getIndicatorVersionVersionById(String indicatorId, Integer versionId, UserDetails userDetails)
            throws LottabyteException {
        Indicator indicator = indicatorRepository.getVersionById(indicatorId, versionId, userDetails);
        if (indicator == null)
            throw new LottabyteException(
                    Message.LBE02401,
                            userDetails.getLanguage(),
                    indicatorId);
        fillIndicatorVersionRelations(indicator, userDetails);
        return indicator;
    }

    private void fillIndicatorVersionRelations(Indicator indicator, UserDetails userDetails) {
        WorkflowableMetadata md = (WorkflowableMetadata) indicator.getMetadata();
        if (md.getAncestorDraftId() != null) {
            indicator.getMetadata().setTags(tagService.getArtifactTags(md.getAncestorDraftId(), userDetails));

            // List<Reference> referenceForAsset =
            // referenceService.getAllReferenceByPublishedIdAndTypeAndVersionId(indicator.getId(),
            // indicator.getMetadata().getVersionId(),
            // String.valueOf(ArtifactType.data_asset), userDetails);
            List<Reference> referenceForAsset = referenceService.getAllReferenceBySourceIdAndTargetType(
                    md.getAncestorDraftId(), String.valueOf(ArtifactType.data_asset), userDetails);
            List<String> assetIdList = referenceForAsset.stream().map(r -> r.getEntity().getTargetId())
                    .collect(Collectors.toList());
            indicator.getEntity().setDataAssetIds(assetIdList);
        } else {
            List<Reference> referenceForAsset = referenceService.getAllReferenceBySourceIdAndTargetType(
                    indicator.getId(), String.valueOf(ArtifactType.data_asset), userDetails);
            indicator.getEntity().setDataAssetIds(
                    referenceForAsset.stream().map(r -> r.getEntity().getTargetId()).collect(Collectors.toList()));
        }
    }

    public List<IndicatorType> getIndicatorTypes(UserDetails userDetails) {
        return indicatorRepository.getIndicatorTypes(userDetails);
    }

    public IndicatorType getIndicatorTypeById(String id, UserDetails userDetails) {
        return indicatorRepository.getIndicatorTypeById(id, userDetails);
    }

    @Override
    public String createDraft(String publishedId, WorkflowState workflowState, WorkflowType workflowType,
            UserDetails userDetails) throws LottabyteException {
        Indicator current = getById(publishedId, userDetails);

        ProcessInstance pi = null;
        String workflowTaskId = null;
        String draftId = UUID.randomUUID().toString();
        if (workflowService.isWorkflowEnabled(serviceArtifactType) && workflowService
                .getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

            pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.UPDATE,
                    userDetails);
            workflowTaskId = pi.getId();

        }
        indicatorRepository.createDraftFromPublished(publishedId, draftId, workflowTaskId, userDetails);

        if (current.getEntity().getDataAssetIds() != null && !current.getEntity().getDataAssetIds().isEmpty()) {
            for (String s : current.getEntity().getDataAssetIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(draftId);
                referenceEntity.setSourceType(ArtifactType.indicator);
                referenceEntity.setTargetId(s);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(ArtifactType.data_asset);
                referenceEntity.setReferenceType(ReferenceType.INDICATOR_TO_DATA_ASSET);
                referenceEntity.setVersionId(0);

                referenceService.createReference(new UpdatableReferenceEntity(referenceEntity), userDetails);
            }
        }

        if (current.getEntity().getTermLinkIds() != null && !current.getEntity().getTermLinkIds().isEmpty()) {
            createTermLinksReference(current.getEntity().getTermLinkIds(), draftId, publishedId, userDetails);
        }

        return draftId;
    }

    public SearchResponse<FlatIndicator> searchIndicatorsByDomain(SearchRequestWithJoin request, String domainId,
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

        SearchResponse<FlatIndicator> res = indicatorRepository.searchIndicatorsByDomain(request, domainId,
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
        return res;
    }

    public EntitySampleDQRule createDQRule(String indicatorId,
            UpdatableEntitySampleDQRule entitySampleDQRule, UserDetails userDetails)
            throws LottabyteException {

        EntitySampleDQRule sampleDQRule = indicatorRepository.createDQRule(indicatorId,
                entitySampleDQRule, userDetails);
        return entitySampleRepository.getSampleDQRule(sampleDQRule.getId(), userDetails);
    }

    public List<EntitySampleDQRule> getDQRules(String indicatorId,
            UserDetails userDetails) throws LottabyteException {

        return entitySampleRepository.getSampleDQRulesByIndicator(indicatorId,
                userDetails);
    }

    public SearchableIndicator getSearchableArtifact(Indicator indicator, UserDetails userDetails) {
        SearchableIndicator searchableIndicator = new SearchableIndicator();
        searchableIndicator.setId(indicator.getMetadata().getId());
        searchableIndicator.setVersionId(indicator.getMetadata().getVersionId());
        searchableIndicator.setName(indicator.getMetadata().getName());
        searchableIndicator.setDescription(indicator.getEntity().getDescription());
        searchableIndicator.setDomainId(indicator.getEntity().getDomainId());
        searchableIndicator.setIndicatorTypeId(indicator.getEntity().getIndicatorTypeId());
        searchableIndicator.setModifiedBy(indicator.getMetadata().getModifiedBy());
        searchableIndicator.setModifiedAt(indicator.getMetadata().getModifiedAt());
        searchableIndicator.setArtifactType(indicator.getMetadata().getArtifactType());
        searchableIndicator.setEffectiveStartDate(indicator.getMetadata().getEffectiveStartDate());
        searchableIndicator.setEffectiveEndDate(indicator.getMetadata().getEffectiveEndDate());
        searchableIndicator.setTags(Helper.getEmptyListIfNull(indicator.getMetadata().getTags()).stream()
                .map(Relation::getName).collect(Collectors.toList()));

        searchableIndicator.setDqChecks(indicator.getEntity().getDqChecks());
        searchableIndicator.setCalcCode(indicator.getEntity().getCalcCode());
        searchableIndicator.setFormula(indicator.getEntity().getFormula());
        searchableIndicator.setExamples(indicator.getEntity().getExamples());
        searchableIndicator.setLink(indicator.getEntity().getLink());
        searchableIndicator.setDataTypeId(indicator.getEntity().getDatatypeId());
        searchableIndicator.setLimits(indicator.getEntity().getLimits());
        searchableIndicator.setLimitsInternal(indicator.getEntity().getLimits_internal());
        searchableIndicator.setRoles(indicator.getEntity().getRoles());

        if (indicator.getEntity().getDomainId() != null && !indicator.getEntity().getDomainId().isEmpty()) {
            Domain d = domainRepository.getById(indicator.getEntity().getDomainId(), userDetails);
            if (d != null)
                searchableIndicator.setDomainName(d.getName());
        }
        if (indicator.getEntity().getIndicatorTypeId() != null
                && !indicator.getEntity().getIndicatorTypeId().isEmpty()) {
            IndicatorType it = getIndicatorTypeById(indicator.getEntity().getIndicatorTypeId(), userDetails);
            if (it != null)
                searchableIndicator.setIndicatorTypeName(it.getName());
        }
        searchableIndicator.setDomains(Collections.singletonList(indicator.getEntity().getDomainId()));
        if (indicator.getEntity().getDatatypeId() != null && !indicator.getEntity().getDatatypeId().isEmpty()) {
            DataType dt = dataTypeRepository.getById(indicator.getEntity().getDatatypeId(), userDetails);
            if (dt != null)
                searchableIndicator.setDataTypeName(dt.getName());
        }
        return searchableIndicator;
    }
}
