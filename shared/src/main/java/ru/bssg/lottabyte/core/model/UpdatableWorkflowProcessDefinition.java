package ru.bssg.lottabyte.core.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdatableWorkflowProcessDefinition {
    private ArtifactType artifactType;
    private ArtifactAction artifactAction;
    private String processDefinitionKey;
    private String description;
}
