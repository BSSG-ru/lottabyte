package ru.bssg.lottabyte.core.model.product;

import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class ProductType extends ModeledObject<ProductTypeEntity> {
    public ProductType() {
    }

    public ProductType(ProductTypeEntity entity) {
        super(entity);
    }

    public ProductType(ProductTypeEntity entity, Metadata md) {
        super(entity, md, ArtifactType.product_type);
    }
}
