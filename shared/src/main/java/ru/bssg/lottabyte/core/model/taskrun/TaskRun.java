package ru.bssg.lottabyte.core.model.taskrun;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class TaskRun extends ModeledObject<TaskRunEntity> {

    public TaskRun() throws LottabyteException {
    }

    public TaskRun(TaskRunEntity entity) throws LottabyteException {
        super(entity);
    }

    public TaskRun(TaskRunEntity entity, Metadata md) throws LottabyteException {
        super(entity, md, ArtifactType.task_run);
    }

}
