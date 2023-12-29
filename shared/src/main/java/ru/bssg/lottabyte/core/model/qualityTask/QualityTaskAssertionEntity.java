package ru.bssg.lottabyte.core.model.qualityTask;

import java.sql.Timestamp;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

@Data
@EqualsAndHashCode(callSuper=false)
public class QualityTaskAssertionEntity extends Entity {

    private String stateName;
    private String state;
    private String ruleName;
    private String column;
    private String msg;
    private String ruleId;
    private String olId;
    private String assertion;

    public QualityTaskAssertionEntity() {
        super(ArtifactType.qualityAssertionTask);
    }

}
