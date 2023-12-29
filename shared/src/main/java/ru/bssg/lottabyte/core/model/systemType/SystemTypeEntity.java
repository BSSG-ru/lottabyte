package ru.bssg.lottabyte.core.model.systemType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper=false)
public class SystemTypeEntity extends Entity {
    private String taskId;
    private String resultSampleId;
    private Integer resultSampleVersionId;
    private String resultMsg;
    private String staredBy;
    private String startMode;
    private LocalDateTime taskStart;
    private LocalDateTime taskEnd;
    private String taskState;
    private LocalDateTime lastUpdated;

    public SystemTypeEntity() {
        super(ArtifactType.task_run);
    }

}
