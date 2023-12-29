package ru.bssg.lottabyte.core.model.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=false)
public class FlatProductSupplyVariant extends FlatModeledObject {
    public FlatProductSupplyVariant(ProductSupplyVariant productSupplyVariant) {
        super(productSupplyVariant.getFlatModeledObject());
    }
}
