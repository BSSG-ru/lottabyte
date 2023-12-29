package ru.bssg.lottabyte.core.model.workflow;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class WorkflowEntity extends Entity {

    private WorkflowType workflowType;
    private List<WorkflowAction> workflowActions;

    public WorkflowEntity() {
        super(ArtifactType.workflow);
    }

}
