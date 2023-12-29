package ru.bssg.lottabyte.core.model.dataentity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.model.FlatModeledObject;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class FlatDataEntityAttribute extends FlatModeledObject {
    private DataEntityAttributeType attributeType;
    private String entityId;
    private List<String> tags;
    private String attributeId;

    public FlatDataEntityAttribute(DataEntityAttribute de) {
        super(de.getFlatModeledObject());
        this.attributeType = de.getEntity().getAttributeType();
        this.entityId = de.getEntity().getEntityId();
        this.attributeId = de.getEntity().getAttributeId();
    }
}
