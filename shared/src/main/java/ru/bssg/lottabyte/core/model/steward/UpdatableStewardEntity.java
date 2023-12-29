package ru.bssg.lottabyte.core.model.steward;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;

import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update steward object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableStewardEntity extends StewardEntity {

    public UpdatableStewardEntity(StewardEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setUserId(fromCopy.getUserId());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        if (fromCopy.getDomains() != null) {
            this.setDomains(new ArrayList<>(fromCopy.getDomains()));
        }
    }

}
