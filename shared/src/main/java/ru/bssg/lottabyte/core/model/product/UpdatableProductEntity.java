package ru.bssg.lottabyte.core.model.product;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update Product object"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdatableProductEntity extends ProductEntity {

    public UpdatableProductEntity(ProductEntity fromCopy) {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        if (fromCopy.getIndicatorIds() != null) {
            this.setIndicatorIds(new ArrayList<>(fromCopy.getIndicatorIds()));
        }
        if (fromCopy.getEntityAttributeIds() != null) {
            this.setEntityAttributeIds(new ArrayList<>(fromCopy.getEntityAttributeIds()));
        }
        this.setDomainId(fromCopy.getDomainId());
        this.setProblem(fromCopy.getProblem());
        this.setConsumer(fromCopy.getConsumer());
        this.setValue(fromCopy.getValue());
        this.setFinanceSource(fromCopy.getFinanceSource());
        if (fromCopy.getProductTypeIds() != null)
            this.setProductTypeIds(new ArrayList<>(fromCopy.getProductTypeIds()));
        if (fromCopy.getProductSupplyVariantIds() != null)
            this.setProductSupplyVariantIds(new ArrayList<>(fromCopy.getProductSupplyVariantIds()));
        if (fromCopy.getDataAssetIds() != null)
            this.setDataAssetIds(new ArrayList<>(fromCopy.getDataAssetIds()));
        this.setLink(fromCopy.getLink());
        this.setLimits(fromCopy.getLimits());
        this.setLimits_internal(fromCopy.getLimits_internal());
        this.setRoles(fromCopy.getRoles());
    }

}
