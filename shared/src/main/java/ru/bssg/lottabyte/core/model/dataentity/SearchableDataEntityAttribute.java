package ru.bssg.lottabyte.core.model.dataentity;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;
import ru.bssg.lottabyte.core.model.search.SearchableCustomAttribute;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class SearchableDataEntityAttribute extends SearchableArtifact {
    protected String entityId;
    protected String enumerationId;
    protected DataEntityAttributeType attributeType;
    protected List<String> mappedSamplePropertyIds;

    @Builder
    public SearchableDataEntityAttribute(String id, Integer versionId, String name, String description, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, String entityId, String enumerationId, DataEntityAttributeType attributeType, List<String> mappedSamplePropertyIds) {
        super(id, versionId, name, description, tags, modifiedBy, modifiedAt, artifactType, effectiveStartDate, effectiveEndDate, customAttributes, domains);
        this.entityId = entityId;
        this.enumerationId = enumerationId;
        this.attributeType = attributeType;
        this.mappedSamplePropertyIds = mappedSamplePropertyIds;
    }
}
