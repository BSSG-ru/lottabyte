package ru.bssg.lottabyte.core.model.task;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

@Data
@EqualsAndHashCode(callSuper=false)
public class TaskScheduleEntity extends Entity {
    private String taskId;
    private Boolean enabled;
    private TaskSchedulerType scheduleType;
    private String scheduleParams;

    public TaskScheduleEntity() { super(ArtifactType.task_schedule); }
}
