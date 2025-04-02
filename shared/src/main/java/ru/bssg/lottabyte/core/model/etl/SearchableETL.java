package ru.bssg.lottabyte.core.model.etl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.model.reference.SearchableReference;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;
import ru.bssg.lottabyte.core.model.search.SearchableCustomAttribute;
import ru.bssg.lottabyte.core.model.search.SearchableRelatedArtifact;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
public class SearchableETL extends SearchableArtifact {
    protected String code;
    protected String algorithm;
    protected String systemId;
    protected String systemName;
    protected String etlTypeId;
    protected String etlTypeName;

    @Builder
    public SearchableETL(String id, Integer versionId, String name, String description, String shortDescription, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, String artifactState, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, String algorithm, String code, String systemId, String systemName, String etlTypeId, String etlTypeName, List<SearchableRelatedArtifact> relatedArtifacts) {
        super(id, versionId, name, description, shortDescription, tags, modifiedBy, modifiedAt, artifactType, artifactState, effectiveStartDate, effectiveEndDate, customAttributes, domains, relatedArtifacts);
        this.code = code;
        this.algorithm = algorithm;
        this.systemId = systemId;
        this.systemName = systemName;
        this.etlTypeId = etlTypeId;
        this.etlTypeName = etlTypeName;
    }
}
