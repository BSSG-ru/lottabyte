package ru.bssg.lottabyte.core.model.qualityTask;

import java.sql.Timestamp;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

@Data
@EqualsAndHashCode(callSuper=false)
public class QualityTaskRunEntity extends Entity {

    private String ruleId;
    private String name;
    private Timestamp time;
    private String state;

    public QualityTaskRunEntity() {
        super(ArtifactType.qualityTaskRun);
    }

}
