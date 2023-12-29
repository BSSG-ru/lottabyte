package ru.bssg.lottabyte.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;

import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Base object for entity"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Entity {

    protected String id;
    protected String name;
    protected String description;
    protected ArtifactType artifactType;

    public String generateGuid() throws LottabyteException {
        return UUID.randomUUID().toString();
    }

    public Entity(ArtifactType artifactType) {
        this.artifactType = artifactType;
    }

}
