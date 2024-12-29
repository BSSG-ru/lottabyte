package ru.bssg.lottabyte.core.model.dataasset;

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
@EqualsAndHashCode(callSuper=false)
public class SearchableDataAsset extends SearchableArtifact {
    protected String systemId;
    protected String domainId;
    protected String entityId;
    protected String systemName;
    protected String domainName;
    protected String entityName;
    protected Integer rowsCount;
    protected Integer dataSize;
    protected Boolean hasQuery;
    protected Boolean hasSample;
    protected Boolean hasStatistics;
    protected String roles;
    protected String techName;

    @Builder
    public SearchableDataAsset(String id, Integer versionId, String name, String description, String shortDescription, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, String artifactState, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, String systemId, String domainId, String entityId, String systemName, String domainName, String entityName, Integer rowsCount, Integer dataSize, Boolean hasQuery, Boolean hasSample, Boolean hasStatistics, String roles, String techName) {
        super(id, versionId, name, description, shortDescription, tags, modifiedBy, modifiedAt, artifactType, artifactState, effectiveStartDate, effectiveEndDate, customAttributes, domains);
        this.systemId = systemId;
        this.domainId = domainId;
        this.entityId = entityId;
        this.systemName = systemName;
        this.domainName = domainName;
        this.entityName = entityName;
        this.rowsCount = rowsCount;
        this.dataSize = dataSize;
        this.hasQuery = hasQuery;
        this.hasSample = hasSample;
        this.hasStatistics = hasStatistics;
        this.roles = roles;
        this.techName = techName;
    }
}
