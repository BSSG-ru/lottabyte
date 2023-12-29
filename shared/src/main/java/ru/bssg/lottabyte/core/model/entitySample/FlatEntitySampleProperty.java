package ru.bssg.lottabyte.core.model.entitySample;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;

import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=false)
public class FlatEntitySampleProperty extends FlatModeledObject {
    private EntitySamplePropertyPathType pathType;
    private String path;
    private List<String> mappedEntityAttributeIds;
    private String entityAttributeName;
    private String entityAttributeId;

    public FlatEntitySampleProperty(EntitySampleProperty entitySampleProperty) {
        super(entitySampleProperty.getFlatModeledObject());
        this.pathType = entitySampleProperty.getEntity().getPathType();
        this.path = entitySampleProperty.getEntity().getPath();
        this.mappedEntityAttributeIds = entitySampleProperty.getEntity().getMappedAttributeIds();
    }
}
