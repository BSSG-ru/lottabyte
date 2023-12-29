package ru.bssg.lottabyte.core.model.system;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;
import ru.bssg.lottabyte.core.model.search.SearchableCustomAttribute;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class SearchableSystem extends SearchableArtifact {
    protected String systemType;
    protected String connectorId;
    protected String systemFolderId;

    @Builder
    public SearchableSystem(String id, Integer versionId, String name, String description, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, String systemType, String connectorId, String systemFolderId) {
        super(id, versionId, name, description, tags, modifiedBy, modifiedAt, artifactType, effectiveStartDate, effectiveEndDate, customAttributes, domains);
        this.systemType = systemType;
        this.connectorId = connectorId;
        this.systemFolderId = systemFolderId;
    }
}
