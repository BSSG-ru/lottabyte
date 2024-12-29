package ru.bssg.lottabyte.core.model.task;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;
import ru.bssg.lottabyte.core.model.relation.Relation;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class TaskEntity extends Entity {

    private String systemConnectionId;
    private String queryId;

    private Boolean isMetadataTask;
    private List<Relation> metadatabases;
    private List<TaskSchedule> schedules;

    public TaskEntity() {
        super(ArtifactType.task);
    }

}
