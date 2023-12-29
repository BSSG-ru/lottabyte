package ru.bssg.lottabyte.core.model.workflow;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WorkflowActionParam {

    private String id;
    private String name;
    private String displayName;
    private String type;
    private Boolean required;

}
