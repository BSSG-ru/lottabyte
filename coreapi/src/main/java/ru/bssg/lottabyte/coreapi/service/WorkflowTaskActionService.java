package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.taskrun.TaskRun;
import ru.bssg.lottabyte.core.model.taskrun.UpdatableTaskRunEntity;
import ru.bssg.lottabyte.core.model.workflow.*;
import ru.bssg.lottabyte.core.model.workflowTaskAction.UpdatableWorkflowTaskActionEntity;
import ru.bssg.lottabyte.core.model.workflowTaskAction.WorkflowTaskAction;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.WorkflowRepository;
import ru.bssg.lottabyte.coreapi.repository.WorkflowTaskActionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ru.bssg.lottabyte.core.i18n.Message.LBE00044;
import static ru.bssg.lottabyte.core.i18n.Message.LBE03008;

@Service
@Slf4j
public class WorkflowTaskActionService {

    private final WorkflowTaskActionRepository workflowTaskActionRepository;
    private final WorkflowService workflowService;

    @Lazy
    @Autowired
    public WorkflowTaskActionService(WorkflowTaskActionRepository workflowTaskActionRepository, WorkflowService workflowService) {
        this.workflowTaskActionRepository = workflowTaskActionRepository;
        this.workflowService = workflowService;
    }


    public List<WorkflowTaskAction> getWorkflowTaskActionListByWorkflowId(String workflowId, UserDetails userDetails) {
        return workflowTaskActionRepository.getWorkflowTaskActionListByWorkflowId(workflowId, userDetails);
    }

    public WorkflowTaskAction getWorkflowTaskActionById(String workflowTaskId, UserDetails userDetails) {
        return workflowTaskActionRepository.getWorkflowTaskActionById(workflowTaskId, userDetails);
    }

    public WorkflowTaskAction updateWorkflowTaskActionById(String workflowTaskId, UpdatableWorkflowTaskActionEntity updatableWorkflowTaskActionEntity, UserDetails userDetails) {
        workflowTaskActionRepository.updateWorkflowTaskActionById(workflowTaskId, updatableWorkflowTaskActionEntity, userDetails);
        return getWorkflowTaskActionById(workflowTaskId, userDetails);
    }

    public WorkflowTaskAction createWorkflowTaskAction(UpdatableWorkflowTaskActionEntity updatableWorkflowTaskActionEntity, List<WorkflowActionParamResult> actionParams, UserDetails userDetails) throws LottabyteException {
        if (updatableWorkflowTaskActionEntity.getWorkflowTaskId() == null || workflowService.getWorkflowTaskById(updatableWorkflowTaskActionEntity.getWorkflowTaskId(), userDetails) == null)
            throw new LottabyteException(Message.LBE03002, userDetails.getLanguage(), updatableWorkflowTaskActionEntity.getWorkflowTaskId());

        String workflowTaskActionId = workflowTaskActionRepository.createWorkflowTaskAction(updatableWorkflowTaskActionEntity, userDetails);
        for(WorkflowActionParamResult workflowActionParamResult : actionParams){
            createWorkflowTaskActionParam(workflowTaskActionId, workflowActionParamResult, userDetails);
        }
        return getWorkflowTaskActionById(workflowTaskActionId, userDetails);
    }

    //workflow param

    public List<WorkflowActionParamResult> getWorkflowTaskActionParamListByWorkflowId(String workflowTaskActionId, UserDetails userDetails) {
        return workflowTaskActionRepository.getWorkflowTaskActionParamListByWorkflowId(workflowTaskActionId, userDetails);
    }
    public List<WorkflowActionParamResult> createWorkflowTaskActionParam(String workflowTaskActionId, WorkflowActionParamResult actionParam, UserDetails userDetails) throws LottabyteException {
        if (workflowTaskActionId == null || getWorkflowTaskActionById(workflowTaskActionId, userDetails) == null)
            throw new LottabyteException(Message.LBE03010, userDetails.getLanguage(), workflowTaskActionId);

        String workflowTaskActionParamId = workflowTaskActionRepository.createWorkflowTaskActionParam(workflowTaskActionId, actionParam, userDetails);
        return getWorkflowTaskActionParamListByWorkflowId(workflowTaskActionId, userDetails);
    }
}
