package ru.bssg.lottabyte.core.model.product;

import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class ProductSupplyVariant extends ModeledObject<ProductSupplyVariantEntity> {
    public ProductSupplyVariant() {
    }

    public ProductSupplyVariant(ProductSupplyVariantEntity entity) {
        super(entity);
    }

    public ProductSupplyVariant(ProductSupplyVariantEntity entity, Metadata md) {
        super(entity, md, ArtifactType.product_supply_variant);
    }
}
