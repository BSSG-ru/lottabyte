package ru.bssg.lottabyte.coreapi.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.FormService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.form.FormProperty;
import org.flowable.engine.form.TaskFormData;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.jooq.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.domain.FlatDomain;
import ru.bssg.lottabyte.core.model.domain.UpdatableDomainEntity;
import ru.bssg.lottabyte.core.model.taskrun.TaskRun;
import ru.bssg.lottabyte.core.model.workflow.*;
import ru.bssg.lottabyte.core.model.workflowTaskAction.UpdatableWorkflowTaskActionEntity;
import ru.bssg.lottabyte.core.model.workflowTaskAction.WorkflowTaskAction;
import ru.bssg.lottabyte.core.model.workflowTaskAction.WorkflowTaskActionEntity;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.WorkflowRepository;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static ru.bssg.lottabyte.core.i18n.Message.*;

@Service
@Slf4j
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowTaskActionService workflowTaskActionService;
    private final DomainService domainService;
    private final SystemService systemService;
    private final EntityService entityService;
    private final DataAssetService dataAssetService;
    private final IndicatorService indicatorService;
    private final BusinessEntityService businessEntityService;
    private final ProductService productService;
    private final DQRuleService dqRuleService;
    private final EntityQueryService entityQueryService;
    private final UserService userService;

    // Flowable services
    private final RuntimeService runtimeService;
    private final IdentityService identityService;
    private final org.flowable.engine.TaskService taskService;
    private final FormService formService;
    private final RepositoryService repositoryService;

    @Lazy
    @Autowired
    public WorkflowService(WorkflowRepository workflowRepository, WorkflowTaskActionService workflowTaskActionService,
            DomainService domainService,
            SystemService systemService, EntityService entityService,
            DataAssetService dataAssetService, IndicatorService indicatorService,
            BusinessEntityService businessEntityService, ProductService productService,
            EntityQueryService entityQueryService, DQRuleService dqRuleService,
            RuntimeService runtimeService, IdentityService identityService,
            org.flowable.engine.TaskService taskService, FormService formService,
            UserService userService, RepositoryService repositoryService) {
        this.workflowRepository = workflowRepository;
        this.workflowTaskActionService = workflowTaskActionService;
        this.domainService = domainService;
        this.systemService = systemService;
        this.entityService = entityService;
        this.dataAssetService = dataAssetService;
        this.indicatorService = indicatorService;
        this.businessEntityService = businessEntityService;
        this.productService = productService;
        this.dqRuleService = dqRuleService;
        this.entityQueryService = entityQueryService;
        this.runtimeService = runtimeService;
        this.identityService = identityService;
        this.taskService = taskService;
        this.formService = formService;
        this.userService = userService;
        this.repositoryService = repositoryService;
    }

    public boolean isWorkflowEnabled(ArtifactType artifactType) {
        switch (artifactType) {
            case domain:
            case system:
            case entity:
            case data_asset:
            case indicator:
            case business_entity:
            case entity_query:
            case product:
            case dq_rule:
                return true;
            default:
                return false;
        }
    }

    public UUID getNewWorkflowTaskUUID() {
        return UUID.randomUUID();
    }

    public Workflow getDefaultWorkflow(ArtifactType artifactType, WorkflowType workflowType, UserDetails userDetails) {
        PaginatedArtifactList<Workflow> workflows = workflowRepository.getAllPaginated(0, 10000, "/v1/workflows/", userDetails);
        return workflows.getResources().stream().filter(x -> workflowType.equals(x.getEntity().getWorkflowType()))
                .findFirst().orElse(null);
    }

    public ProcessInstance startFlowableProcess(String artifactId, ArtifactType artifactType, ArtifactAction artifactAction,
                                       UserDetails ud) throws LottabyteException {
        List<WorkflowProcessDefinition> prDefs = workflowRepository.getWorkflowProcessDefinitions(ud);
        WorkflowProcessDefinition prdef = prDefs.stream()
                .filter(x -> x.getArtifactAction().equals(artifactAction) && x.getArtifactType().equals(artifactType))
                .findFirst().orElse(null);
        if (prdef == null)
            throw new LottabyteException(LBE03012, ud.getLanguage(), artifactType, artifactAction);
        if (!repositoryService.createProcessDefinitionQuery().latestVersion().list().stream().anyMatch(x -> x.getKey().equals(prdef.getProcessDefinitionKey())))
            throw new LottabyteException(LBE03013, ud.getLanguage(), prdef.getProcessDefinitionKey(), artifactType, artifactAction);
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("artifact_id",artifactId);
        variables.put("artifact_type",artifactType.getText());
        variables.put("created_by", ud.getUid());
        identityService.setAuthenticatedUserId(ud.getUid());
        //return runtimeService.startProcessInstanceByKey("lottabyteOneStepApproval", variables);
        return runtimeService.startProcessInstanceByKey(prdef.getProcessDefinitionKey(), variables);

    }

    public ProcessInstance startFlowableRemoveProcess(String artifactId, ArtifactType artifactType,
                                                String userId) {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("artifact_id",artifactId);
        variables.put("artifact_type",artifactType.getText());
        variables.put("created_by", userId);
        identityService.setAuthenticatedUserId(userId);
        return runtimeService.startProcessInstanceByKey("lottabyteOneStepRemoval", variables);
    }

    public ModeledObject createDraft(String artifactType, String artifactId, UserDetails userDetails)
            throws LottabyteException {
        if (!EnumUtils.isValidEnum(ArtifactType.class, artifactType))
            throw new LottabyteException(LBE00044, userDetails.getLanguage(), artifactType);
        if (!isWorkflowEnabled(ArtifactType.valueOf(artifactType)))
            throw new LottabyteException(LBE03008, userDetails.getLanguage(), artifactType);
        IWorkflowableService wfService = getWorkflowServiceByType(ArtifactType.valueOf(artifactType));
        if (!wfService.existsInState(artifactId, ArtifactState.PUBLISHED, userDetails))
            throw new LottabyteException(
                    Message.LBE03009,
                            userDetails.getLanguage(),
                    artifactId);
        String newId = wfService.createDraft(artifactId, WorkflowState.NOT_STARTED, WorkflowType.PUBLISH, userDetails);
        return wfService.getById(newId, userDetails);
    }

    public WorkflowTask postCreateDraft(String workflowTaskId, WorkflowType worflowType, String artifactId,
            ArtifactType artifactType, WorkflowState workflowState, UserDetails userDetails) {
        if (!isWorkflowEnabled(artifactType))
            return null;
        Workflow workflow = getDefaultWorkflow(artifactType, worflowType, userDetails);
        if (workflow == null)
            return null;
        String wfTaskId = workflowRepository.createWorkflowTask(workflowTaskId, artifactId, artifactType,
                workflow.getId(), workflowState, userDetails);
        if (wfTaskId == null)
            return null;
        return workflowRepository.getWorkflowTaskById(wfTaskId, userDetails);
    }

    public WorkflowTask getWorkflowTaskById(String workflowTaskId, UserDetails userDetails) {
        return getWorkflowTaskById(workflowTaskId, userDetails, false);
    }

    public WorkflowTask getWorkflowTaskById(String workflowTaskId, UserDetails userDetails, Boolean includeActions) {
        WorkflowTask res = null; //workflowRepository.getWorkflowTaskById(workflowTaskId, userDetails);
        if (res != null && res.getEntity() != null) {
            if (includeActions != null && includeActions)
                res.getEntity().setActions(getWorkflowTaskActionsById(workflowTaskId, userDetails));
        } else {
            List<String> userRoleNames = userDetails.getUserRoles().stream()
                    .map(x -> userService.getUserRoleById(x, userDetails.getTenant()).getName())
                    .collect(Collectors.toList());
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(workflowTaskId)
                    .taskCandidateGroupIn(userRoleNames)
                    .list();
            if (tasks == null || tasks.isEmpty()) {
                List<Task> tasksAll = taskService.createTaskQuery().processInstanceId(workflowTaskId)
                        .list();
            }

            if (tasks != null && !tasks.isEmpty()) {
                Task task = tasks.get(0);

                Map<String, Object> variables = taskService.getVariables(task.getId());
                TaskFormData tfd = formService.getTaskFormData(task.getId());
                WorkflowTaskEntity entity = new WorkflowTaskEntity();
                entity.setArtifactId((String)variables.get("artifact_id"));
                entity.setArtifactType(ArtifactType.fromString((String)variables.get("artifact_type")));
                entity.setWorkflowId(task.getProcessDefinitionId());
                entity.setWorkflowState(task.getName());

                List<String> parts = new ArrayList<>();
                List<IdentityLink> links = taskService.getIdentityLinksForTask(task.getId());
                if (links != null) {
                    for (IdentityLink il : links) {
                        if (IdentityLinkType.CANDIDATE.equals(il.getType())) {
                            if (il.getGroupId() != null) {
                                List<UserDetails> users = userService.getUsersByRoleName(il.getGroupId(), userDetails.getTenant());
                                if (users != null) {
                                    for (UserDetails ud : users) {
                                        parts.add(ud.getDisplayName());
                                    }
                                }
                            }
                        }
                    }
                }

                if (!parts.isEmpty())
                    entity.setResponsible(StringUtils.join(parts, ", "));

                Metadata md = new Metadata();
                md.setId(task.getId());
                md.setCreatedBy((String)variables.get("created_by"));
                md.setCreatedAt(task.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                res = new WorkflowTask(entity, md);
                if (tfd != null && tfd.getFormProperties() != null && !tfd.getFormProperties().isEmpty()) {
                    FormProperty actionProperty = tfd.getFormProperties().stream()
                            .filter(x -> "action".equals(x.getId()) && x.getType().getName().equals("enum"))
                            .findFirst().orElse(null);
                    List<FormProperty> othersProperties = tfd.getFormProperties().stream()
                            .filter(x -> !"action".equals(x.getId()))
                            .collect(Collectors.toList());
                    if (actionProperty != null) {
                        Map<String, String> vals = (Map<String, String>)actionProperty.getType().getInformation("values");
                        for (String enumId : vals.keySet()) {
                            WorkflowAction action = new WorkflowAction();
                            action.setId(enumId);
                            action.setWorkflowTaskId(task.getId());
                            action.setDisplayName(vals.get(enumId));
                            action.setPostUrl("/v1/workflows/tasks/" + task.getId() + "/actions/" + action.getId());
                            for (FormProperty otherProperty : othersProperties) {
                                WorkflowActionParam p = new WorkflowActionParam();
                                p.setId(otherProperty.getId());
                                p.setName(otherProperty.getName());
                                p.setDisplayName(otherProperty.getName());
                                p.setRequired(otherProperty.isRequired());
                                p.setType(otherProperty.getType().getName().toUpperCase());
                                if (action.getParams() == null)
                                    action.setParams(new ArrayList<>());
                                action.getParams().add(p);
                            }
                            if (res.getEntity().getActions() == null)
                                res.getEntity().setActions(new ArrayList<>());
                            res.getEntity().getActions().add(action);
                        }
                    }
                }
            } else {
                //res.getEntity().setActions(new ArrayList<>());
            }
        }
        return res;
    }

    public List<WorkflowAction> getWorkflowTaskActionsById(String workflowTaskId, UserDetails userDetails) {
        WorkflowTask wfTask = workflowRepository.getWorkflowTaskById(workflowTaskId, userDetails);
        if (wfTask.getEntity().getWorkflowId() != null) {
            Workflow wf = workflowRepository.getById(wfTask.getEntity().getWorkflowId(), userDetails);
            if (WorkflowType.PUBLISH.equals(wf.getEntity().getWorkflowType())) {
                WorkflowActionParam p = new WorkflowActionParam();
                p.setId(UUID.randomUUID().toString());
                p.setName("description");
                p.setDisplayName("Введите описание");
                p.setRequired(true);
                p.setType("STRING");
                WorkflowAction a = new WorkflowAction();
                a.setParams(new ArrayList<>());
                a.getParams().add(p);
                a.setId("cancel");
                a.setWorkflowTaskId(workflowTaskId);
                a.setDisplayName("Отменить изменения");
                a.setPostUrl("/v1/workflows/tasks/" + workflowTaskId + "/actions/" + a.getId());
                List<WorkflowAction> res = new ArrayList<>();
                res.add(a);

                p = new WorkflowActionParam();
                p.setId(UUID.randomUUID().toString());
                p.setName("description");
                p.setDisplayName("Введите описание");
                p.setRequired(true);
                p.setType("STRING");
                a = new WorkflowAction();
                a.setParams(new ArrayList<>());
                a.getParams().add(p);
                a.setId("publish");
                a.setWorkflowTaskId(workflowTaskId);
                a.setDisplayName("Опубликовать");
                a.setPostUrl("/v1/workflows/tasks/" + workflowTaskId + "/actions/" + a.getId());
                res.add(a);
                return res;
            } else if (WorkflowType.REMOVE.equals(wf.getEntity().getWorkflowType())) {
                WorkflowActionParam p = new WorkflowActionParam();
                p.setId(UUID.randomUUID().toString());
                p.setName("description");
                p.setDisplayName("Введите описание");
                p.setRequired(false);
                p.setType("STRING");
                WorkflowAction a = new WorkflowAction();
                a.setParams(new ArrayList<>());
                a.getParams().add(p);
                a.setId("cancel");
                a.setWorkflowTaskId(workflowTaskId);
                a.setDisplayName("Отменить удаление");
                a.setPostUrl("/v1/workflows/tasks/" + workflowTaskId + "/actions/" + a.getId());
                List<WorkflowAction> res = new ArrayList<>();
                res.add(a);

                p = new WorkflowActionParam();
                p.setId(UUID.randomUUID().toString());
                p.setName("description");
                p.setDisplayName("Введите описание");
                p.setRequired(true);
                p.setType("STRING");
                a = new WorkflowAction();
                a.setParams(new ArrayList<>());
                a.getParams().add(p);
                a.setId("approve_removal");
                a.setWorkflowTaskId(workflowTaskId);
                a.setDisplayName("Согласовать удаление");
                a.setPostUrl("/v1/workflows/tasks/" + workflowTaskId + "/actions/" + a.getId());
                res.add(a);
                return res;
            }
        }
        return new ArrayList<>();
    }

    private IWorkflowableService getWorkflowServiceByType(ArtifactType artifactType) {
        IWorkflowableService res = null;
        if (ArtifactType.domain.equals(artifactType)) {
            return domainService;
        } else if (ArtifactType.system.equals(artifactType)) {
            return systemService;
        } else if (ArtifactType.entity.equals(artifactType)) {
            return entityService;
        } else if (ArtifactType.data_asset.equals(artifactType)) {
            return dataAssetService;
        } else if (ArtifactType.indicator.equals(artifactType)) {
            return indicatorService;
        } else if (ArtifactType.business_entity.equals(artifactType)) {
            return businessEntityService;
        } else if (ArtifactType.entity_query.equals(artifactType)) {
            return entityQueryService;
        } else if (ArtifactType.product.equals(artifactType)) {
            return productService;

        } else if (ArtifactType.dq_rule.equals(artifactType)) {
            return dqRuleService;

        } else {
            return null;
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public WorkflowActionResultWrapper<?> postWorkflowTaskAction(String workflowTaskId, String workflowAction,
            List<WorkflowActionParamResult> actionParams, UserDetails userDetails) throws LottabyteException {

        WorkflowTask wfTask = workflowRepository.getWorkflowTaskById(workflowTaskId, userDetails);
        WorkflowActionResultWrapper res = null;

        if (wfTask != null) {
            // Internal workflow engine
            WorkflowTaskActionEntity workflowTaskActionEntity = new WorkflowTaskActionEntity();
            workflowTaskActionEntity.setWorkflowAction(workflowAction);
            workflowTaskActionEntity.setWorkflowTaskId(workflowTaskId);
            UpdatableWorkflowTaskActionEntity updatableWorkflowTaskActionEntity = new UpdatableWorkflowTaskActionEntity(
                    workflowTaskActionEntity);
            WorkflowTaskAction workflowTaskAction = workflowTaskActionService
                    .createWorkflowTaskAction(updatableWorkflowTaskActionEntity, actionParams, userDetails);

            if (wfTask.getEntity().getArtifactId() != null && wfTask.getEntity().getArtifactType() != null) {

                List<WorkflowAction> wfActions = getWorkflowTaskActionsById(workflowTaskId, userDetails);
                if (wfActions.stream().map(x -> x.getId()).noneMatch(y -> y.equals(workflowAction)))
                    throw new LottabyteException(
                            Message.LBE03007,
                                    userDetails.getLanguage(),
                            workflowAction);

                IWorkflowableService wfService = getWorkflowServiceByType(wfTask.getEntity().getArtifactType());
                if (workflowAction.equals("publish")) {
                    res = new WorkflowActionResultWrapper<>(
                            wfService.wfPublish(wfTask.getEntity().getArtifactId(), userDetails), true);
                } else if (workflowAction.equals("cancel")) {
                    wfService.wfCancel(wfTask.getEntity().getArtifactId(), userDetails);
                    res = new WorkflowActionResultWrapper<>(null, true);
                } else if (workflowAction.equals("approve_removal")) {
                    if (!WorkflowState.MARKED_FOR_REMOVAL.name().equals(wfTask.getEntity().getWorkflowState()))
                        throw new LottabyteException(
                                Message.LBE03005,
                                        userDetails.getLanguage());
                    wfService.wfApproveRemoval(wfTask.getEntity().getArtifactId(), userDetails);
                    res = new WorkflowActionResultWrapper<>(null, true);
                }

            }
            if (res != null)
                workflowRepository.deleteWorkflowTask(workflowTaskId, userDetails);
        } else {
            // Flowable wokrflow engine
            List<Task> tasks = taskService.createTaskQuery().taskId(workflowTaskId).list();
            if (tasks != null && !tasks.isEmpty()) {
                Task task = tasks.get(0);
                Map<String, Object> wfVariables = taskService.getVariables(task.getId());
                IWorkflowableService wfService = getWorkflowServiceByType(ArtifactType.fromString((String)wfVariables.get("artifact_type")));
                String artifactId = (String)wfVariables.get("artifact_id");
                TaskFormData tfd = formService.getTaskFormData(task.getId());
                List<String> formActions = new ArrayList<>();
                if (tfd != null && tfd.getFormProperties() != null && !tfd.getFormProperties().isEmpty()) {
                    FormProperty actionProperty = tfd.getFormProperties().stream()
                            .filter(x -> "action".equals(x.getId()) && x.getType().getName().equals("enum"))
                            .findFirst().orElse(null);
                    if (actionProperty != null) {
                        Map<String, String> vals = (Map<String, String>) actionProperty.getType().getInformation("values");
                        vals.keySet().stream().forEach(x -> formActions.add(x));
                    }
                }
                if (!formActions.contains(workflowAction))
                    throw new LottabyteException(
                            Message.LBE03007,
                                    userDetails.getLanguage(),
                            workflowAction);
                Map<String, Object> variables = new HashMap<String, Object>();
                variables.put("action", workflowAction);
                variables.put("ud_uid", userDetails.getUid());
                variables.put("ud_tenant", userDetails.getTenant());
                variables.put("ud_stewardid", userDetails.getStewardId());
                taskService.complete(task.getId(), variables);
                ModeledObject mo = wfService.getById(artifactId, userDetails);
                if (mo != null) {
                    WorkflowableMetadata wfMetadata = (WorkflowableMetadata)mo.getMetadata();
                    if (wfMetadata.getState() != null && wfMetadata.getState() == ArtifactState.DRAFT_HISTORY) {
                        String newId = wfService.getIdByAncestorDraftId(artifactId, userDetails);
                        if (newId != null) {
                            mo = wfService.getById(newId, userDetails);
                        } else {
                            mo = null;
                        }
                    }
                }
                return new WorkflowActionResultWrapper<>(mo, true);
            } else {
                // No workflow task found in any of the engines
                throw new LottabyteException(
                        Message.LBE03002,
                                userDetails.getLanguage(),
                        workflowTaskId);
            }
        }
        return res;
    }

    public String getWorkflowStateName(String state, UserDetails userDetails) {
        return workflowRepository.getWorkflowStateName(state, userDetails);
    }

    public SearchResponse<FlatWorkflowProcessDefinition> searchSettings(SearchRequestWithJoin request, UserDetails userDetails)
            throws LottabyteException {
        SearchColumn[] searchableColumns = {
                new SearchColumn("description", SearchColumn.ColumnType.Text),
                new SearchColumn("at.name", SearchColumn.ColumnType.Text),
                new SearchColumn("artifact_action", SearchColumn.ColumnType.Text),
        };

        SearchColumnForJoin[] joinColumns = {};

        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        ServiceUtils.validateSearchRequestWithJoinState(request, userDetails);
        SearchResponse<FlatWorkflowProcessDefinition> res = workflowRepository.searchSettings(request, searchableColumns, userDetails);

        return res;
    }

    public WorkflowProcessDefinition deleteSettingsById(String id, UserDetails userDetails) throws LottabyteException {
        if (!workflowRepository.settingsExists(id, userDetails))
            throw new LottabyteException(
                    Message.LBE03301,
                            userDetails.getLanguage(),
                    id);
        workflowRepository.deleteSettingsById(id, userDetails);
        return null;
    }

    public WorkflowProcessDefinition getSettingsById(String id, UserDetails userDetails) throws LottabyteException {
        WorkflowProcessDefinition pd = workflowRepository.getSettingsById(id, userDetails);
        if (pd == null)
            throw new LottabyteException(LBE03301, userDetails.getLanguage(), id);

        return pd;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public WorkflowProcessDefinition updateSettings(String id, UpdatableWorkflowProcessDefinition pd, UserDetails userDetails)
            throws LottabyteException {
        if (!workflowRepository.settingsExists(id, userDetails))
            throw new LottabyteException(
                    LBE03301,
                            userDetails.getLanguage(),
                    id);

        workflowRepository.updateSettings(id, pd, userDetails);

        return workflowRepository.getSettingsById(id, userDetails);
    }

    @Transactional
    public WorkflowProcessDefinition createSettings(UpdatableWorkflowProcessDefinition pd, UserDetails userDetails) throws LottabyteException {

        String id = workflowRepository.createSettings(pd, userDetails);

        return workflowRepository.getSettingsById(id, userDetails);


    }

    public Map<String, String> getFlowableProcessDefinitions(UserDetails userDetails) {
        ProcessDefinitionQuery q = repositoryService.createProcessDefinitionQuery();
        Map<String, String> res = new HashMap<>();
        for (ProcessDefinition pd : q.latestVersion().list()) {
            res.put(pd.getKey(), pd.getName());
        }
        return res;
    }
}
