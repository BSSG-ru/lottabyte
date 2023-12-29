package ru.bssg.lottabyte.core.model.dqRule;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.model.reference.SearchableReference;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import ru.bssg.lottabyte.core.model.search.SearchableCustomAttribute;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
public class SearchableDQRule extends SearchableArtifact {

    protected String ruleRef;
    protected String settings;
    private String ruleTypeId;
    private String ruleTypeName;

    @Builder
    public SearchableDQRule(String id, Integer versionId, String name, String description, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, String ruleRef, String settings, String ruleTypeId, String ruleTypeName) {
        super(id, versionId, name, description, tags, modifiedBy, modifiedAt, artifactType, effectiveStartDate, effectiveEndDate, customAttributes, domains);
        this.ruleRef = ruleRef;
        this.settings = settings;
        this.ruleTypeId = ruleTypeId;
        this.ruleTypeName = ruleTypeName;
    }
}
