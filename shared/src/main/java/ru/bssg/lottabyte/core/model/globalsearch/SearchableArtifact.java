package ru.bssg.lottabyte.core.model.globalsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchableArtifact {
    private SearchableMetadata metadata;
    private SearchableEntity entity;
    private GsiCategory categories;
    private List<GsiCustomAttributesDefinition> customAttributes;
    @JsonProperty("_score")
    private double score;

    public SearchableArtifact(SearchableMetadata metadata, SearchableEntity entity) {
        this.metadata = metadata;
        this.entity = entity;
    }
}
