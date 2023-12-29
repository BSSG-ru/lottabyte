package ru.bssg.lottabyte.core.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper=true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WorkflowProcessDefinition {
    private String id;
    private ArtifactType artifactType;
    private String artifactTypeName;
    private ArtifactAction artifactAction;
    private String processDefinitionKey;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;

}
