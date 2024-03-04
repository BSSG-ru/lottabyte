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
import ru.bssg.lottabyte.core.model.businessEntity.BusinessEntity;
import ru.bssg.lottabyte.core.model.dataasset.DataAsset;
import ru.bssg.lottabyte.core.model.dqRule.DQRule;
import ru.bssg.lottabyte.core.model.dqRule.FlatDQRule;
import ru.bssg.lottabyte.core.model.dqRule.SearchableDQRule;
import ru.bssg.lottabyte.core.model.dqRule.UpdatableDQRuleEntity;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRuleEntity;
import ru.bssg.lottabyte.core.model.entitySample.UpdatableEntitySampleDQRule;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;
import ru.bssg.lottabyte.core.model.workflow.WorkflowTask;
import ru.bssg.lottabyte.core.model.workflow.WorkflowType;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.DQRuleRepository;
import ru.bssg.lottabyte.coreapi.repository.DataAssetRepository;
import ru.bssg.lottabyte.coreapi.repository.SystemRepository;
import ru.bssg.lottabyte.coreapi.util.Helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DQRuleService extends WorkflowableService<DQRule> {
    private final DQRuleRepository dqRuleRepository;
    private final TagService tagService;
    private final ElasticsearchService elasticsearchService;

    private final WorkflowService workflowService;
    private final ArtifactType serviceArtifactType = ArtifactType.dq_rule;

    private final SearchColumn[] searchableColumns = {
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            new SearchColumn("rule_ref", SearchColumn.ColumnType.Text),
            new SearchColumn("settings", SearchColumn.ColumnType.Text),
            new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
            new SearchColumn("stewards", SearchColumn.ColumnType.Array),
            new SearchColumn("tags", SearchColumn.ColumnType.Text),
            new SearchColumn("workflow_state", SearchColumn.ColumnType.Text)
    };

    private final SearchColumnForJoin[] joinColumns = {};

    @Autowired
    public DQRuleService(DQRuleRepository dqRuleRepository, TagService tagService,
            ElasticsearchService elasticsearchService, WorkflowService workflowService) {
        super(dqRuleRepository, workflowService, tagService, ArtifactType.dq_rule, elasticsearchService);
        this.dqRuleRepository = dqRuleRepository;
        this.tagService = tagService;
        this.elasticsearchService = elasticsearchService;
        this.workflowService = workflowService;
    }

    public boolean existsInLog(String ruleId, UserDetails userDetails) throws LottabyteException {
        return dqRuleRepository.existsInLog(ruleId, userDetails);
    }

    // Wf interface

    public boolean existsInState(String artifactId, ArtifactState artifactState, UserDetails userDetails)
            throws LottabyteException {
        DQRule dqRule = getDQRuleById(artifactId, userDetails);
        WorkflowableMetadata md = (WorkflowableMetadata) dqRule.getMetadata();
        if (!artifactState.equals(md.getState()))
            return false;
        return true;
    }

    public String getDraftArtifactId(String publishedId, UserDetails userDetails) {
        return dqRuleRepository.getDraftId(publishedId, userDetails);
    }

    public void wfCancel(String draftDQRuleId, UserDetails userDetails) throws LottabyteException {
        DQRule current = dqRuleRepository.getById(draftDQRuleId, userDetails);
        if (current == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftDQRuleId);
        if (!ArtifactState.DRAFT.equals(((WorkflowableMetadata) current.getMetadata()).getState()))
            throw new LottabyteException(
                    Message.LBE03003, userDetails.getLanguage());
        dqRuleRepository.setStateById(draftDQRuleId, ArtifactState.DRAFT_HISTORY, userDetails);
    }

    public void wfApproveRemoval(String draftDQRuleId, UserDetails userDetails) throws LottabyteException {
        DQRule current = dqRuleRepository.getById(draftDQRuleId, userDetails);
        if (current == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftDQRuleId);
        String publishedId = ((WorkflowableMetadata) current.getMetadata()).getPublishedId();
        if (publishedId == null)
            throw new LottabyteException(
                    Message.LBE03006,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftDQRuleId);
        dqRuleRepository.setStateById(current.getId(), ArtifactState.DRAFT_HISTORY, userDetails);
        dqRuleRepository.setStateById(publishedId, ArtifactState.REMOVED, userDetails);
        elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(publishedId), userDetails);
    }

    public DQRule wfPublish(String draftDQRuleId, UserDetails userDetails) throws LottabyteException {
        DQRule draft = dqRuleRepository.getById(draftDQRuleId, userDetails);
        String publishedId = ((WorkflowableMetadata) draft.getMetadata()).getPublishedId();
        if (draft == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftDQRuleId);
        if (dqRuleRepository.dqRuleIdNameExists(draft.getEntity().getName(), publishedId, userDetails))
            throw new LottabyteException(
                    Message.LBE00108,
                            userDetails.getLanguage(),
                    draft.getEntity().getName());

        if (publishedId == null) {
            String newPublishedId = dqRuleRepository.publishDQRuleDraft(draftDQRuleId, null, userDetails);

            tagService.mergeTags(draftDQRuleId, serviceArtifactType, newPublishedId, serviceArtifactType, userDetails);
            DQRule d = dqRuleRepository.getById(newPublishedId, userDetails);
            elasticsearchService.insertElasticSearchEntity(
                    Collections.singletonList(getSearchableArtifact(d, userDetails)),
                    userDetails);
            return d;
        } else {
            dqRuleRepository.publishDQRuleDraft(draftDQRuleId, publishedId, userDetails);
            DQRule currentPublished = dqRuleRepository.getById(publishedId, userDetails);

            tagService.mergeTags(draftDQRuleId, serviceArtifactType, publishedId, serviceArtifactType, userDetails);
            DQRule d = dqRuleRepository.getById(publishedId, userDetails);
            elasticsearchService.updateElasticSearchEntity(
                    Collections.singletonList(getSearchableArtifact(d, userDetails)),
                    userDetails);
            return d;
        }
    }

    public DQRule getById(String id, UserDetails userDetails) throws LottabyteException {
        return getDQRuleById(id, userDetails);
    }

    public DQRule getDQRuleById(String dqRuleId, UserDetails userDetails) throws LottabyteException {
        DQRule dqRule = dqRuleRepository.getById(dqRuleId, userDetails);
        if (dqRule == null)
            throw new LottabyteException(Message.LBE00101,
                            userDetails.getLanguage(), dqRuleId);
        WorkflowableMetadata md = (WorkflowableMetadata) dqRule.getMetadata();
        if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
            md.setDraftId(dqRuleRepository.getDraftId(md.getId(), userDetails));
        dqRule.getMetadata().setTags(tagService.getArtifactTags(dqRuleId, userDetails));
        return dqRule;
    }

    public DQRule getDQRuleByIdAndState(String dqRuleId, UserDetails userDetails) throws LottabyteException {
        DQRule dqRule = dqRuleRepository.getByIdAndState(dqRuleId, ArtifactState.PUBLISHED.name(), userDetails);
        if (dqRule == null)
            throw new LottabyteException(Message.LBE00101,
                            userDetails.getLanguage(), dqRuleId);
        WorkflowableMetadata md = (WorkflowableMetadata) dqRule.getMetadata();
        if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
            md.setDraftId(dqRuleRepository.getDraftId(md.getId(), userDetails));
        dqRule.getMetadata().setTags(tagService.getArtifactTags(dqRuleId, userDetails));
        return dqRule;
    }

    public DQRule getDQRuleVersionById(String dqRuleId, Integer versionId, UserDetails userDetails)
            throws LottabyteException {
        DQRule dqRule = dqRuleRepository.getVersionById(dqRuleId, versionId, userDetails);
        if (dqRule == null)
            throw new LottabyteException(Message.LBE00103,
                            userDetails.getLanguage(), dqRuleId, versionId);
        fillDQRuleRelations(dqRule, userDetails);
        return dqRule;
    }

    public DQRule deleteDQRuleById(String dqRuleId, UserDetails userDetails) throws LottabyteException {
        if (!dqRuleRepository.existsById(dqRuleId,
                new ArtifactState[] { ArtifactState.PUBLISHED, ArtifactState.DRAFT }, userDetails))
            throw new LottabyteException(
                    Message.LBE00001,
                            userDetails.getLanguage(),
                    dqRuleId);

        if (dqRuleRepository.dqRuleEntityExists(dqRuleId, userDetails))
            throw new LottabyteException(
                    Message.LBE03205,
                            userDetails.getLanguage(),
                    dqRuleId);

        DQRule current = dqRuleRepository.getById(dqRuleId, userDetails);
        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {

            String draftId = dqRuleRepository.getDraftId(dqRuleId, userDetails);
            
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE03419,
                                userDetails.getLanguage(),
                        draftId);
            draftId = createDraftDQRule(current, WorkflowState.MARKED_FOR_REMOVAL, userDetails);
            return dqRuleRepository.getById(draftId, userDetails);
        } else {
            dqRuleRepository.deleteById(dqRuleId, userDetails);
            tagService.deleteAllTagsByArtifactId(dqRuleId, userDetails);
            return null;
        }
        // elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(dqRuleId),
        // userDetails);
    }

    public DQRule createDQRule(UpdatableDQRuleEntity dqRuleEntity, UserDetails userDetails) throws LottabyteException {
        if (dqRuleEntity.getName() == null || dqRuleEntity.getName().isEmpty())
            throw new LottabyteException(
                    Message.LBE00104, userDetails.getLanguage());
        if (dqRuleEntity.getRuleTypeId() == null || dqRuleEntity.getRuleTypeId().isEmpty())
            throw new LottabyteException(Message.LBE02405,
                            userDetails.getLanguage(), dqRuleEntity.getName());
        if (dqRuleRepository.getRuleTypeById(dqRuleEntity.getRuleTypeId(), userDetails) == null)
            throw new LottabyteException(Message.LBE02405,
                            userDetails.getLanguage(), dqRuleEntity.getName());

        String workflowTaskId = null;
        ProcessInstance pi = null;

        dqRuleEntity.setId(UUID.randomUUID().toString());

        if (workflowService.isWorkflowEnabled(serviceArtifactType)
                && workflowService.getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

            pi = workflowService.startFlowableProcess(dqRuleEntity.getId(), serviceArtifactType,
                    ArtifactAction.CREATE,
                    userDetails);
            workflowTaskId = pi.getId();

        }

        String newDQRuleId = dqRuleRepository.createDQRule(dqRuleEntity, workflowTaskId, userDetails);
        DQRule dqRule = dqRuleRepository.getById(newDQRuleId, userDetails);
        workflowService.postCreateDraft(workflowTaskId, WorkflowType.PUBLISH, newDQRuleId, serviceArtifactType,
                WorkflowState.NOT_STARTED, userDetails);
        return dqRule;
    }

    public void patchDQRuleByIndicatorIdAndRuleId(String indicatorId, String dqRuleId, UpdatableEntitySampleDQRule updatableEntitySampleDQRule, UserDetails userDetails) {
        dqRuleRepository.patchDQRuleByIndicatorIdAndRuleId(indicatorId, dqRuleId, updatableEntitySampleDQRule, userDetails);
    }

    public void patchDQRuleLinkById(String id, UpdatableEntitySampleDQRule updatableEntitySampleDQRule, UserDetails userDetails) {
        dqRuleRepository.patchDQRuleLinkById(id, updatableEntitySampleDQRule, userDetails);
    }

    public void createDQRuleLink(EntitySampleDQRuleEntity entity, UserDetails userDetails) {
        dqRuleRepository.createDQRuleLink(entity, userDetails);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public DQRule updateDQRule(String dqRuleId, UpdatableDQRuleEntity dqRuleEntity, UserDetails userDetails)
            throws LottabyteException {

        DQRule current = dqRuleRepository.getById(dqRuleId, userDetails);
        String draftId = null;
        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            draftId = dqRuleRepository.getDraftId(dqRuleId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE03419,
                                userDetails.getLanguage(),
                        draftId);
        }

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
            dqRuleRepository.createDraftFromPublished(dqRuleId, draftId, workflowTaskId, userDetails);
            tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

        } else {
            draftId = dqRuleId;
        }
        dqRuleRepository.updateDQRule(draftId, dqRuleEntity, userDetails);
        return dqRuleRepository.getById(draftId, userDetails);

    }

    private String createDraftDQRule(DQRule current, WorkflowState workflowState, UserDetails userDetails)
            throws LottabyteException {
        ProcessInstance pi = null;
        String draftId = null;
        String workflowTaskId = null;

        draftId = UUID.randomUUID().toString();
        pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.REMOVE, userDetails);
        workflowTaskId = pi.getId();

        draftId = dqRuleRepository.createDQRuleDraft(current.getId(), draftId, workflowTaskId, userDetails);
        tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

        return draftId;
    }

    public PaginatedArtifactList<DQRule> getDQRulesPaginated(Integer offset, Integer limit, String artifactState,
            UserDetails userDetails) throws LottabyteException {
        if (!EnumUtils.isValidEnum(ArtifactState.class, artifactState))
            throw new LottabyteException(
                    Message.LBE00067,
                            userDetails.getLanguage(),
                    artifactState);
        PaginatedArtifactList<DQRule> dqRulePaginatedArtifactList = dqRuleRepository.getAllPaginated(offset, limit, "/v1/dq_rules/",
                ArtifactState.valueOf(artifactState), userDetails);
        dqRulePaginatedArtifactList.getResources().forEach(
                dqRule -> dqRule.getMetadata().setTags(tagService.getArtifactTags(dqRule.getId(), userDetails)));
        return dqRulePaginatedArtifactList;
    }

    public PaginatedArtifactList<DQRule> getDQRuleVersions(String dqRuleId, Integer offset, Integer limit,
            UserDetails userDetails) throws LottabyteException {
        if (!dqRuleRepository.existsById(dqRuleId,
                new ArtifactState[] { ArtifactState.PUBLISHED, ArtifactState.REMOVED }, userDetails)) {
            PaginatedArtifactList<DQRule> res = new PaginatedArtifactList<>();
            res.setCount(0);
            res.setOffset(offset);
            res.setLimit(limit);
            res.setResources(new ArrayList<>());
            return res;
            // throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.LBE00101,
            // dqRuleId);
        }
        PaginatedArtifactList<DQRule> dqRulePaginatedArtifactList = dqRuleRepository.getVersionsById(dqRuleId, offset,
                limit, "/v1/dq_rules/" + dqRuleId + "/versions", userDetails);
        // List<Tag> tagList = tagService.getArtifactTags(dqRuleId, userDetails);
        // dqRulePaginatedArtifactList.getResources().forEach(dqRule ->
        // dqRule.getMetadata().setTags(tagList));
        for (DQRule d : dqRulePaginatedArtifactList.getResources()) {
            // d.getMetadata().setTags(ta);
            fillDQRuleRelations(d, userDetails);
        }
        return dqRulePaginatedArtifactList;
    }

    private void fillDQRuleRelations(DQRule d, UserDetails userDetails) {
        WorkflowableMetadata md = (WorkflowableMetadata) d.getMetadata();
        if (md.getAncestorDraftId() != null) {
            d.getMetadata().setTags(tagService.getArtifactTags(md.getAncestorDraftId(), userDetails));
        }
    }

    public SearchResponse<FlatDQRule> searchDQRules(SearchRequestWithJoin request, UserDetails userDetails)
            throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        ServiceUtils.validateSearchRequestWithJoinState(request, userDetails);
        SearchResponse<FlatDQRule> res = dqRuleRepository.searchDQRules(request, searchableColumns, userDetails);
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

    public SearchableDQRule getSearchableArtifact(DQRule dqRule, UserDetails userDetails) {
        SearchableDQRule sa = new SearchableDQRule();

        sa.setId(dqRule.getMetadata().getId());
        sa.setVersionId(dqRule.getMetadata().getVersionId());
        sa.setName(dqRule.getMetadata().getName());
        sa.setDescription(dqRule.getEntity().getDescription());
        sa.setModifiedBy(dqRule.getMetadata().getModifiedBy());
        sa.setModifiedAt(dqRule.getMetadata().getModifiedAt());
        sa.setArtifactType(dqRule.getMetadata().getArtifactType());
        sa.setEffectiveStartDate(dqRule.getMetadata().getEffectiveStartDate());
        sa.setEffectiveEndDate(dqRule.getMetadata().getEffectiveEndDate());
        sa.setTags(Helper.getEmptyListIfNull(dqRule.getMetadata().getTags()).stream()
                .map(x -> x.getName()).collect(Collectors.toList()));
        sa.setRuleTypeId(dqRule.getEntity().getRuleTypeId());
        if (dqRule.getEntity().getRuleTypeId() != null
                && !dqRule.getEntity().getRuleTypeId().isEmpty()) {
            DQRuleType it = getRuleTypeById(dqRule.getEntity().getRuleTypeId(), userDetails);
            if (it != null)
                sa.setRuleTypeName(it.getName());
        }

        return sa;
    }

    @Override
    public String createDraft(String publishedId, WorkflowState workflowState, WorkflowType workflowType,
            UserDetails userDetails) throws LottabyteException {
        DQRule current = getDQRuleById(publishedId, userDetails);

        ProcessInstance pi = null;
        String workflowTaskId = null;
        String draftId = UUID.randomUUID().toString();
        if (workflowService.isWorkflowEnabled(serviceArtifactType) && workflowService
                .getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

            pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.UPDATE,
                    userDetails);
            workflowTaskId = pi.getId();

        }
        dqRuleRepository.createDraftFromPublished(publishedId, draftId, workflowTaskId, userDetails);

        tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

        return draftId;
    }

    public List<DQRuleType> getRuleTypes(UserDetails userDetails) {
        return dqRuleRepository.getRuleTypes(userDetails);
    }

    public DQRuleType getRuleTypeById(String id, UserDetails userDetails) {
        return dqRuleRepository.getRuleTypeById(id, userDetails);
    }

    public Integer getLastHistoryIdByPublishedId(String publishedId, UserDetails userDetails) {
        return dqRuleRepository.getLastHistoryIdByPublishedId(publishedId, userDetails);
    }

    public void deleteDQRuleLinkById(String id, UserDetails userDetails) {
        dqRuleRepository.deleteDQRuleLinkById(id, userDetails);
    }
}
