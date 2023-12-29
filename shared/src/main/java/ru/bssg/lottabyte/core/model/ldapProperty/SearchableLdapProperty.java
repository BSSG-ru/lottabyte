package ru.bssg.lottabyte.core.model.ldapProperty;

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
public class SearchableLdapProperty extends SearchableArtifact {
    private String providerUrl;
    private String principal;
    private String credentials;
    private String base_dn;
    private String user_query;
    private Integer tenantId;

    @Builder
    public SearchableLdapProperty(String id, Integer versionId, String name, String description, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, String providerUrl, String principal, String credentials, String base_dn, String user_query, Integer tenantId) {
        super(id, versionId, name, description, tags, modifiedBy, modifiedAt, artifactType, effectiveStartDate, effectiveEndDate, customAttributes, domains);
        this.providerUrl = providerUrl;
        this.principal = principal;
        this.credentials = credentials;
        this.base_dn = base_dn;
        this.user_query = user_query;
        this.tenantId = tenantId;
    }
}
