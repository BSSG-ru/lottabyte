package ru.bssg.lottabyte.core.model.workflow;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WorkflowActionParamResult {
    private String id;
    private String paramName;
    private String paramType;
    private String paramValue;
}
