package ru.bssg.lottabyte.core.model.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.tag.SearchableTag;
import ru.bssg.lottabyte.core.model.tag.TagEntity;

import java.util.Objects;
import java.util.stream.Collectors;

public class Task extends ModeledObject<TaskEntity> {

    public Task() {
    }

    public Task(TaskEntity entity) {
        super(entity);
    }

    public Task(TaskEntity entity, Metadata md) {
        super(entity, md, ArtifactType.task);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task myObject = (Task) o;
        return Objects.equals(this.getEntity().getScheduleType(), myObject.getEntity().getScheduleType()) &&
               Objects.equals(this.getEntity().getScheduleParams(), myObject.getEntity().getScheduleParams());
    }
}
