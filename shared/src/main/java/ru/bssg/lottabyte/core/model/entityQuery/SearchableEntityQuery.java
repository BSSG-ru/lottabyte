package ru.bssg.lottabyte.core.model.entityQuery;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;
import ru.bssg.lottabyte.core.model.search.SearchableCustomAttribute;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class SearchableEntityQuery extends SearchableArtifact {
    protected String queryText;
    protected String entityId;
    protected String systemId;
    protected String entityName;
    protected String systemName;

    @Builder
    public SearchableEntityQuery(String id, Integer versionId, String name, String description, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, String queryText, String entityId, String systemId, String entityName, String systemName) {
        super(id, versionId, name, description, tags, modifiedBy, modifiedAt, artifactType, effectiveStartDate, effectiveEndDate, customAttributes, domains);
        this.queryText = queryText;
        this.entityId = entityId;
        this.systemId = systemId;
        this.entityName = entityName;
        this.systemName = systemName;
    }
}
