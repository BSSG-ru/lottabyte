package ru.bssg.lottabyte.core.model.task;

import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

import java.util.Objects;

public class TaskSchedule extends ModeledObject<TaskScheduleEntity> {
    public TaskSchedule() {
    }

    public TaskSchedule(TaskScheduleEntity entity) {
        super(entity);
    }

    public TaskSchedule(TaskScheduleEntity entity, Metadata md) {
        super(entity, md, ArtifactType.task);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskSchedule myObject = (TaskSchedule) o;
        return Objects.equals(this.getEntity().getScheduleType(), myObject.getEntity().getScheduleType()) &&
                Objects.equals(this.getEntity().getScheduleParams(), myObject.getEntity().getScheduleParams());
    }
}
