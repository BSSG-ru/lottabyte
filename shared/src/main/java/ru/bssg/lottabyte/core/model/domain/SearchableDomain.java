package ru.bssg.lottabyte.core.model.domain;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;
import ru.bssg.lottabyte.core.model.search.SearchableCustomAttribute;
import ru.bssg.lottabyte.core.model.search.SearchableRelatedArtifact;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class SearchableDomain extends SearchableArtifact {
    protected List<String> stewards;

    @Builder
    public SearchableDomain(String id, Integer versionId, String name, String description, String shortDescription, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, String artifactState, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, List<String> stewards, List<SearchableRelatedArtifact> relatedArtifacts) {
        super(id, versionId, name, description, shortDescription, tags, modifiedBy, modifiedAt, artifactType, artifactState, effectiveStartDate, effectiveEndDate, customAttributes, domains, relatedArtifacts);
        this.stewards = stewards;
    }
}
