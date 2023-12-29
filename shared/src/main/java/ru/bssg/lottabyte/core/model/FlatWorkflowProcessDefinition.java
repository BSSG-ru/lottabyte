package ru.bssg.lottabyte.core.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@EqualsAndHashCode(callSuper=false)
public class FlatWorkflowProcessDefinition extends FlatModeledObject {
    private ArtifactType artifactType;
    private String artifactTypeName;
    private ArtifactAction artifactAction;
    private String processDefinitionKey;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
    private String id;

    public FlatWorkflowProcessDefinition(WorkflowProcessDefinition d) {
        super();
        this.artifactType = d.getArtifactType();
        this.artifactTypeName = d.getArtifactTypeName();
        this.artifactAction = d.getArtifactAction();
        this.processDefinitionKey = d.getProcessDefinitionKey();
        this.description = d.getDescription();
        this.createdBy = d.getCreatedBy();
        this.createdAt = d.getCreatedAt();
        this.modifiedAt = d.getModifiedAt();
        this.modifiedBy = d.getModifiedBy();
    }
}
