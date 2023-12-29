package ru.bssg.lottabyte.core.model.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

public class ProductSupplyVariantEntity extends Entity {
    @JsonIgnore
    private Integer versionId;

    public ProductSupplyVariantEntity() {
        super(ArtifactType.product_supply_variant);
    }
}
