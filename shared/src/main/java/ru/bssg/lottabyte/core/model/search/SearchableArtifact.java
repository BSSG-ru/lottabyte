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
import ru.bssg.lottabyte.core.model.ArtifactType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchableArtifact {
    protected String id;
    private Integer versionId;
    private String name;
    private String description;
    private String shortDescription;
    private List<String> tags;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
    private String artifactType;
    private String artifactTypeDisplayName;
    private String artifactState;
    private LocalDateTime effectiveStartDate;
    private LocalDateTime effectiveEndDate;
    private List<SearchableCustomAttribute> customAttributes;
    private List<String> domains;
    private List<SearchableRelatedArtifact> relatedArtifacts;

    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
        this.artifactTypeDisplayName = ArtifactType.valueOf(artifactType).getDisplayName();
    }

    public void addRelatedArtifact(String artifactType, String id) {
        if (id == null || id.isEmpty())
            return;
        relatedArtifacts.add(new SearchableRelatedArtifact(artifactType, id));
    }

    public void addRelatedArtifacts(String artifactType, List<String> ids) {
        if (ids == null)
            return;
        for (String id: ids)
            addRelatedArtifact(artifactType, id);
    }

    public SearchableArtifact(String id, Integer versionId, String name, String description, String shortDescription, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, String artifactState, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains) {
        this.id = id;
        this.versionId = versionId;
        this.name = name;
        this.description = description;
        this.shortDescription = shortDescription;
        this.tags = tags;
        this.modifiedBy = modifiedBy;
        this.modifiedAt = modifiedAt;
        this.artifactType = artifactType;
        this.artifactTypeDisplayName = ArtifactType.valueOf(artifactType).getDisplayName();
        this.artifactState = artifactState;
        this.effectiveStartDate = effectiveStartDate;
        this.effectiveEndDate = effectiveEndDate;
        this.customAttributes = customAttributes;
        this.domains = domains;
        this.relatedArtifacts = new ArrayList<>();
    }

    public SearchableArtifact(String id, Integer versionId, String name, String description, String shortDescription, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, String artifactState, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, List<SearchableRelatedArtifact> relatedArtifacts) {
        this.id = id;
        this.versionId = versionId;
        this.name = name;
        this.description = description;
        this.shortDescription = shortDescription;
        this.tags = tags;
        this.modifiedBy = modifiedBy;
        this.modifiedAt = modifiedAt;
        this.artifactType = artifactType;
        this.artifactTypeDisplayName = ArtifactType.valueOf(artifactType).getDisplayName();
        this.artifactState = artifactState;
        this.effectiveStartDate = effectiveStartDate;
        this.effectiveEndDate = effectiveEndDate;
        this.customAttributes = customAttributes;
        this.domains = domains;
        this.relatedArtifacts = relatedArtifacts;
    }
}
