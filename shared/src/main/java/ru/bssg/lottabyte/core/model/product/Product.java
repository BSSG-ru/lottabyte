package ru.bssg.lottabyte.core.model.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRule;

import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper=false)
public class Product extends ModeledObject<ProductEntity> {
    private List<String> entityAttributeIds;
    private List<String> indicatorIds;
    private String domainId;
    private String problem;
    private String consumer;
    private String value;
    private String financeSource;
    private List<String> productTypeIds;
    private List<String> productSupplyVariantIds;
    private List<EntitySampleDQRule> dqRules;

    public Product() {
    }

    public Product(ProductEntity entity) {
        super(entity);
    }

    public Product(ProductEntity entity, Metadata md) {
        super(entity, md, ArtifactType.product);
    }

}
