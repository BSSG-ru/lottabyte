package ru.bssg.lottabyte.core.model.workflowTaskAction;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper=false)
public class WorkflowTaskActionEntity extends Entity {
    private String workflowAction;
    private String workflowTaskId;

    public WorkflowTaskActionEntity() {
        super(ArtifactType.workflow_task_action);
    }

}
