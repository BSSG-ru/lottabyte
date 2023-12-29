package ru.bssg.lottabyte.core.model.indicator;

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

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
public class SearchableIndicator extends SearchableArtifact {

    protected String calcCode;
    protected String formula;
    protected List<String> dqChecks;
    protected List<SearchableReference> entityAttributes;
    protected String domainId;
    protected String domainName;
    protected String indicatorTypeId;
    protected String indicatorTypeName;
    protected String examples;
    protected String link;
    protected String dataTypeId;
    protected String dataTypeName;
    protected String limits;
    protected String limitsInternal;
    protected String roles;

    @Builder
    public SearchableIndicator(String id, Integer versionId, String name, String description, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, String calcCode, String formula, List<String> dqChecks, List<SearchableReference> entityAttributes, String domainId, String domainName, String indicatorTypeId, String indicatorTypeName, String examples, String link, String dataTypeId, String dataTypeName, String limits, String limitsInternal, String roles) {
        super(id, versionId, name, description, tags, modifiedBy, modifiedAt, artifactType, effectiveStartDate, effectiveEndDate, customAttributes, domains);
        this.calcCode = calcCode;
        this.formula = formula;
        this.dqChecks = dqChecks;
        this.entityAttributes = entityAttributes;
        this.domainId = domainId;
        this.domainName = domainName;
        this.indicatorTypeId = indicatorTypeId;
        this.indicatorTypeName = indicatorTypeName;
        this.examples = examples;
        this.link = link;
        this.dataTypeId = dataTypeId;
        this.dataTypeName = dataTypeName;
        this.limits = limits;
        this.limitsInternal = limitsInternal;
        this.roles = roles;
    }
}
