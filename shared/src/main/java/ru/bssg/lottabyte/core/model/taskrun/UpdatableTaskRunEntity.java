package ru.bssg.lottabyte.core.model.taskrun;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update task run object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableTaskRunEntity extends TaskRunEntity {

    public UpdatableTaskRunEntity(TaskRunEntity fromCopy) {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setLastUpdated(fromCopy.getLastUpdated());
        this.setTaskId(fromCopy.getTaskId());
        this.setTaskEnd(fromCopy.getTaskEnd());
        this.setTaskState(fromCopy.getTaskState());
        this.setTaskStart(fromCopy.getTaskStart());
        this.setResultMsg(fromCopy.getResultMsg());
        this.setResultSampleId(fromCopy.getResultSampleId());
        this.setResultSampleVersionId(fromCopy.getResultSampleVersionId());
        this.setStaredBy(fromCopy.getStaredBy());
        this.setStartMode(fromCopy.getStartMode());
    }

}
