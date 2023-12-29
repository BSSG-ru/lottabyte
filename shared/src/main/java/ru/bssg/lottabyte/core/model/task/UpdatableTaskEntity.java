package ru.bssg.lottabyte.core.model.task;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update task object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableTaskEntity extends TaskEntity {

    public UpdatableTaskEntity(TaskEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setSystemConnectionId(fromCopy.getSystemConnectionId());
        this.setQueryId(fromCopy.getQueryId());
        this.setEnabled(fromCopy.getEnabled());
        this.setScheduleType(fromCopy.getScheduleType());
        this.setScheduleParams(fromCopy.getScheduleParams());
    }

}
