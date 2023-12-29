package ru.bssg.lottabyte.core.model.task;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

@Data
@EqualsAndHashCode(callSuper=false)
public class TaskEntity extends Entity {

    private String systemConnectionId;
    private String queryId;
    private Boolean enabled;
    private TaskSchedulerType scheduleType;
    private String scheduleParams;

    public TaskEntity() {
        super(ArtifactType.task);
    }

}
