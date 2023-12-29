package ru.bssg.lottabyte.core.model.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRule;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class ProductEntity extends Entity {
    private List<String> entityAttributeIds;
    private List<String> indicatorIds;
    private String domainId;

    private String problem;
    private String consumer;
    private String value;
    private String financeSource;
    private List<String> productTypeIds;
    private List<String> productSupplyVariantIds;
    private List<String> dataAssetIds;
    private List<EntitySampleDQRule> dqRules;
    private String link;
    private String limits;
    private String limits_internal;
    private String roles;
    private List<String> termLinkIds;

    @JsonIgnore
    private Integer versionId;//Ппереместить в Entity сущность что бы лубая последующая связка с reference работала с версиями

    public ProductEntity() {
        super(ArtifactType.product);
    }

}
