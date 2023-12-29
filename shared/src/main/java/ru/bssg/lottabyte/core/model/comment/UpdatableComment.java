package ru.bssg.lottabyte.core.model.comment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.model.ArtifactType;

import java.time.LocalDateTime;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdatableComment {
    private String id;
    private String commentText;
    private String parentCommentId;
    private String artifactId;
    private String artifactType;
}
