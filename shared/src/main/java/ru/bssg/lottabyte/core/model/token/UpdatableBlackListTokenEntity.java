package ru.bssg.lottabyte.core.model.token;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update BlackListToken object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableBlackListTokenEntity extends BlackListTokenEntity {

    public UpdatableBlackListTokenEntity(BlackListTokenEntity fromCopy) {
        this.setArtifactType(fromCopy.getArtifactType());
        this.setApiTokenId(fromCopy.getApiTokenId());
        this.setCause(fromCopy.getCause());
    }

}
