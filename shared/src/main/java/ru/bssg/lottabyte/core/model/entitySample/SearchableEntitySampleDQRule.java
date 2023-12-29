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
public class SearchableEntitySampleDQRule extends SearchableArtifact {
    protected String id;
    protected String entitySampleId;
    protected String dqRuleId;
    protected String settings;
    protected boolean disabled;
    protected String indicatorId;
    protected String productId;
    protected boolean sendMail;

    @Builder
    public SearchableEntitySampleDQRule(String id, Integer versionId, String name, String description, List<String> tags, String modifiedBy, LocalDateTime modifiedAt, String artifactType, LocalDateTime effectiveStartDate, LocalDateTime effectiveEndDate, List<SearchableCustomAttribute> customAttributes, List<String> domains, String id1, String entitySampleId, String dqRuleId, String settings, boolean disabled, String indicatorId, String productId, boolean sendMail) {
        super(id, versionId, name, description, tags, modifiedBy, modifiedAt, artifactType, effectiveStartDate, effectiveEndDate, customAttributes, domains);
        this.id = id1;
        this.entitySampleId = entitySampleId;
        this.dqRuleId = dqRuleId;
        this.settings = settings;
        this.disabled = disabled;
        this.indicatorId = indicatorId;
        this.productId = productId;
        this.sendMail = sendMail;
    }
}
