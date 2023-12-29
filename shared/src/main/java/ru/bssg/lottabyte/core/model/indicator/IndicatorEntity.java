package ru.bssg.lottabyte.core.model.indicator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRule;
import ru.bssg.lottabyte.core.model.reference.Reference;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class IndicatorEntity extends Entity {

    private String calcCode;
    private String formula;
    private List<String> dqChecks;
    private List<String> dataAssetIds;
    private String domainId;
    private String indicatorTypeId;
    private List<EntitySampleDQRule> dqRules;
    private String examples;
    private String link;
    private String datatypeId;
    private String limits;
    private String limits_internal;
    private String roles;
    private List<String> termLinkIds;

    public IndicatorEntity() {
        super(ArtifactType.indicator);
    }

}
