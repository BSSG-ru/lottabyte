package ru.bssg.lottabyte.core.model.businessEntity;

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
public class SearchableBusinessEntity extends SearchableArtifact {

    protected String techName;
    protected String definition;
    protected String regulation;
    protected List<String> altNames;
    protected List<String> synonymIds;
    protected List<String> beLinkIds;
    protected String domainId;
    protected String domainName;
    protected String formula;
    protected String examples;
    protected String link;
    protected String datatypeId;
    protected String limits;
    protected String roles;

    @Builder
    public SearchableBusinessEntity(String id, Integer versionId, String name, String description, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, String techName, String definition, String regulation, List<String> altNames, List<String> synonymIds, List<String> beLinkIds, String domainId, String domainName, String formula, String examples, String link, String datatypeId, String limits, String roles) {
        super(id, versionId, name, description, tags, modifiedBy, modifiedAt, artifactType, effectiveStartDate, effectiveEndDate, customAttributes, domains);
        this.techName = techName;
        this.definition = definition;
        this.regulation = regulation;
        this.altNames = altNames;
        this.synonymIds = synonymIds;
        this.beLinkIds = beLinkIds;
        this.domainId = domainId;
        this.domainName = domainName;
        this.formula = formula;
        this.examples = examples;
        this.link = link;
        this.datatypeId = datatypeId;
        this.limits = limits;
        this.roles = roles;
    }
}
