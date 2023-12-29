package ru.bssg.lottabyte.core.model.dataasset;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;
import ru.bssg.lottabyte.core.model.ca.CustomAttribute;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRule;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class DataAssetEntity extends Entity {

    private String systemId;
    private String domainId;
    private String entityId;
    private Integer rowsCount;
    private Integer dataSize;
    private Boolean hasQuery;
    private Boolean hasSample;
    private Boolean hasStatistics;
    private List<CustomAttribute> customAttributes;
    private List<EntitySampleDQRule> dqRules;
    private String roles;

    public DataAssetEntity() {
        super(ArtifactType.data_asset);
    }

}
