package ru.bssg.lottabyte.core.model.workflow;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import ru.bssg.lottabyte.core.model.ModeledObject;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class WorkflowActionResultWrapper<T extends ModeledObject>  {
    private Boolean success;
    private T item;

    public WorkflowActionResultWrapper(T item, Boolean success) {
        this.item = item;
        this.success = success;
    }

}
