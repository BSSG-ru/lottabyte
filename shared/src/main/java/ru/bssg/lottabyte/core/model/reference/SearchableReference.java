package ru.bssg.lottabyte.core.model.reference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;
import ru.bssg.lottabyte.core.model.search.SearchableCustomAttribute;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class SearchableReference extends SearchableArtifact {

    protected String sourceId;
    protected String sourceType;
    protected String targetId;
    protected String targetType;
    protected String relationType;
    protected Integer versionId;

    @Builder
    public SearchableReference(String id, Integer versionId, String name, String description, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, String sourceId, String sourceType, String targetId, String targetType, String relationType, Integer versionId1) {
        super(id, versionId, name, description, tags, modifiedBy, modifiedAt, artifactType, effectiveStartDate, effectiveEndDate, customAttributes, domains);
        this.sourceId = sourceId;
        this.sourceType = sourceType;
        this.targetId = targetId;
        this.targetType = targetType;
        this.relationType = relationType;
        this.versionId = versionId1;
    }
}
