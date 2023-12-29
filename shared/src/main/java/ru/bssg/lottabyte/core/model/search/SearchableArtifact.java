package ru.bssg.lottabyte.core.model.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchableArtifact {
    protected String id;
    private Integer versionId;
    private String name;
    private String description;
    private List<String> tags;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
    private String artifactType;
    private LocalDateTime effectiveStartDate;
    private LocalDateTime effectiveEndDate;
    private List<SearchableCustomAttribute> customAttributes;
    private List<String> domains;
}
