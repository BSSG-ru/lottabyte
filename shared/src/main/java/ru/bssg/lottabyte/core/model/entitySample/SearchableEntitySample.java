package ru.bssg.lottabyte.core.model.entitySample;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;
import ru.bssg.lottabyte.core.model.search.SearchableCustomAttribute;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class SearchableEntitySample extends SearchableArtifact {
    protected String entityId;
    protected String systemId;
    protected String entityQueryId;
    protected EntitySampleType sampleType;
    protected String sampleBody;
    protected Boolean isMain;
    protected List<String> propertyNames;
    protected String roles;

    @Builder
    public SearchableEntitySample(String id, Integer versionId, String name, String description, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, String entityId, String systemId, String entityQueryId, EntitySampleType sampleType, String sampleBody, Boolean isMain, List<String> propertyNames, String roles) {
        super(id, versionId, name, description, tags, modifiedBy, modifiedAt, artifactType, effectiveStartDate, effectiveEndDate, customAttributes, domains);
        this.entityId = entityId;
        this.systemId = systemId;
        this.entityQueryId = entityQueryId;
        this.sampleType = sampleType;
        this.sampleBody = sampleBody;
        this.isMain = isMain;
        this.propertyNames = propertyNames;
        this.roles = roles;
    }
}
