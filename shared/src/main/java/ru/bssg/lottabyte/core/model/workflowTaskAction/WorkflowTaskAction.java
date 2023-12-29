package ru.bssg.lottabyte.core.model.workflowTaskAction;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class WorkflowTaskAction extends ModeledObject<WorkflowTaskActionEntity> {

    public WorkflowTaskAction() throws LottabyteException {
    }

    public WorkflowTaskAction(WorkflowTaskActionEntity entity) throws LottabyteException {
        super(entity);
    }

    public WorkflowTaskAction(WorkflowTaskActionEntity entity, Metadata md) throws LottabyteException {
        super(entity, md, ArtifactType.workflow_task_action);
    }

}
