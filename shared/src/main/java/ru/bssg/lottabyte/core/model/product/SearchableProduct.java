package ru.bssg.lottabyte.core.model.product;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;
import ru.bssg.lottabyte.core.model.search.SearchableCustomAttribute;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class SearchableProduct extends SearchableArtifact {
    protected List<String> entityAttributeIds;
    protected List<String> indicatorIds;
    protected List<String> indicatorNames;
    protected String domainId;
    protected String problem;
    protected String consumer;
    protected String value;
    protected String financeSource;
    protected List<String> productTypeIds;
    protected List<String> productTypeNames;
    protected List<String> productSupplyVariantIds;
    protected List<String> productSupplyVariantNames;
    private String link;
    private String limits;
    private String limits_internal;
    private String roles;

    @Builder
    public SearchableProduct(String id, Integer versionId, String name, String description, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, List<String> entityAttributeIds, List<String> indicatorIds, List<String> indicatorNames, String domainId, String problem, String consumer, String value, String financeSource, List<String> productTypeIds, List<String> productTypeNames, List<String> productSupplyVariantIds, List<String> productSupplyVariantNames, String link, String limits, String limits_internal, String roles) {
        super(id, versionId, name, description, tags, modifiedBy, modifiedAt, artifactType, effectiveStartDate, effectiveEndDate, customAttributes, domains);
        this.entityAttributeIds = entityAttributeIds;
        this.indicatorIds = indicatorIds;
        this.indicatorNames = indicatorNames;
        this.domainId = domainId;
        this.problem = problem;
        this.consumer = consumer;
        this.value = value;
        this.financeSource = financeSource;
        this.productTypeIds = productTypeIds;
        this.productTypeNames = productTypeNames;
        this.productSupplyVariantIds = productSupplyVariantIds;
        this.productSupplyVariantNames = productSupplyVariantNames;
        this.link = link;
        this.limits = limits;
        this.limits_internal = limits_internal;
        this.roles = roles;
    }
}
