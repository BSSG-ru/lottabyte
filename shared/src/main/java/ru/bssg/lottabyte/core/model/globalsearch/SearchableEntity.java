package ru.bssg.lottabyte.core.model.globalsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchableEntity {
    SearchableArtifactsEntity artifacts;
}
