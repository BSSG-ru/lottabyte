package ru.bssg.lottabyte.core.model.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

public class ProductTypeEntity extends Entity {
    @JsonIgnore
    private Integer versionId;

    public ProductTypeEntity() {
        super(ArtifactType.product_type);
    }
}
