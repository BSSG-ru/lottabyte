package ru.bssg.lottabyte.core.model.workflow;

import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class WorkflowTask extends ModeledObject<WorkflowTaskEntity> {

    public WorkflowTask() {
    }

    public WorkflowTask(WorkflowTaskEntity entity) { super(entity); }

    public WorkflowTask(WorkflowTaskEntity entity, Metadata md) {
        super(entity, md, ArtifactType.workflow_task);
    }

}
