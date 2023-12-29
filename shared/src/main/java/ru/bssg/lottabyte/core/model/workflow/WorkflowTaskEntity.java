package ru.bssg.lottabyte.core.model.workflow;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class WorkflowTaskEntity extends Entity {

    private String artifactId;
    private ArtifactType artifactType;
    private String workflowId;
    private String workflowState;
    private List<WorkflowAction> actions;
    private String responsible;

    public WorkflowTaskEntity() {
        super(ArtifactType.workflow_task);
    }

}
