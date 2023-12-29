package ru.bssg.lottabyte.core.model.workflow;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WorkflowAction {

    private String id;
    private String workflowTaskId;
    private String displayName;
    private String description;
    private String postUrl;
    private List<WorkflowActionParam> params;

}
