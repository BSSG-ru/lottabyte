package ru.bssg.lottabyte.core.model.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.FlatRelation;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=false)
public class FlatProduct extends FlatModeledObject {
    private List<String> entityAttributeIds;
    private List<String> indicatorIds;
    private String domainId;
    private List<String> tags;
    private String domainName;
    private List<FlatRelation> productTypes;
    private static <T2> List<T2> getEmptyListIfNull(List<T2> list) {
        return (List)(list == null ? new ArrayList() : list);
    }

    public FlatProduct(Product product) {
        super(product.getFlatModeledObject());
        this.entityAttributeIds = product.getEntity().getEntityAttributeIds();
        this.indicatorIds = product.getEntity().getIndicatorIds();
        this.domainId = product.getEntity().getDomainId();
    }
}
