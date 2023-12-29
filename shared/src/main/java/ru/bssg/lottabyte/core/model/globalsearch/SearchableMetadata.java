package ru.bssg.lottabyte.core.model.globalsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import ru.bssg.lottabyte.core.model.ManagedEntityState;

import java.time.ZonedDateTime;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchableMetadata {
    private String name;
    private String description;
    private List<String> tags;
    private String modifiedBy;
    private ZonedDateTime modifiedOn;
    private ManagedEntityState state;
    private String artifactType;
    List<String> stewardIds;
    List<String> terms;
    List<String> termGlobalIds;
    List<String> classifications;
    List<String> classificationGlobalIds;
}


