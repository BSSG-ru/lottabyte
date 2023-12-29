package ru.bssg.lottabyte.core.model.workflowTaskAction;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update WorkflowTaskAction object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableWorkflowTaskActionEntity extends WorkflowTaskActionEntity {

    public UpdatableWorkflowTaskActionEntity(WorkflowTaskActionEntity fromCopy) {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setWorkflowTaskId(fromCopy.getWorkflowTaskId());
        this.setWorkflowAction(fromCopy.getWorkflowAction());
    }

}
