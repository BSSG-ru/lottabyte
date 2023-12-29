package ru.bssg.lottabyte.core.model.entitySample;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;
import ru.bssg.lottabyte.core.model.search.SearchableCustomAttribute;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class SearchableEntitySampleProperty extends SearchableArtifact {
    protected EntitySamplePropertyPathType pathType;
    protected String path;
    protected String entitySampleId;
    protected String entitySampleName;
    protected List<String> mappedAttributeIds;

    @Builder
    public SearchableEntitySampleProperty(String id, Integer versionId, String name, String description, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, EntitySamplePropertyPathType pathType, String path, String entitySampleId, String entitySampleName, List<String> mappedAttributeIds) {
        super(id, versionId, name, description, tags, modifiedBy, modifiedAt, artifactType, effectiveStartDate, effectiveEndDate, customAttributes, domains);
        this.pathType = pathType;
        this.path = path;
        this.entitySampleId = entitySampleId;
        this.entitySampleName = entitySampleName;
        this.mappedAttributeIds = mappedAttributeIds;
    }
}
