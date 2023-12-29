package ru.bssg.lottabyte.core.model.enumeration;

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
        description = "Update enumeration entity"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UpdatableEnumerationEntity extends EnumerationEntity {
    private List<String> variants;

    public UpdatableEnumerationEntity(EnumerationEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        if (fromCopy.getVariants() != null) {
            this.setVariants(new ArrayList<>(fromCopy.getVariants()));
        }
    }
}
