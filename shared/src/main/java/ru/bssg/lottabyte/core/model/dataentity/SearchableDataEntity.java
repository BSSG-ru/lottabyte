package ru.bssg.lottabyte.core.model.dataentity;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ca.CustomAttribute;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;
import ru.bssg.lottabyte.core.model.search.SearchableCustomAttribute;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class SearchableDataEntity extends SearchableArtifact {
    protected String entityFolderId;
    protected List<String> systemIds;
    protected List<String> systemNames;
    protected String businessEntityId;
    protected String businessEntityName;
    protected List<String> attributeNames;
    protected List<String> attributeDescriptions;
    protected String roles;
    protected String techName;

    @Builder
    public SearchableDataEntity(String id, Integer versionId, String name, String description, String shortDescription, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, String artifactState, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, String entityFolderId, List<String> systemIds, List<String> systemNames, String businessEntityId, String businessEntityName, List<String> attributeNames, List<String> attributeDescriptions, String roles, String techName) {
        super(id, versionId, name, description, shortDescription, tags, modifiedBy, modifiedAt, artifactType, artifactState, effectiveStartDate, effectiveEndDate, customAttributes, domains);
        this.entityFolderId = entityFolderId;
        this.systemIds = systemIds;
        this.systemNames = systemNames;
        this.businessEntityId = businessEntityId;
        this.businessEntityName = businessEntityName;
        this.attributeNames = attributeNames;
        this.attributeDescriptions = attributeDescriptions;
        this.roles = roles;
        this.techName = techName;
    }
}
