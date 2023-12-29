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
public class FlatEntitySample extends FlatModeledObject {
    private String systemId;
    private String entityId;
    private String entityQueryId;
    private String systemName;
    private String entityName;
    private String entityQueryName;
    private Boolean isMain;
    private List<String> tags;

    public FlatEntitySample(EntitySample d) {
        super(d.getFlatModeledObject());
        this.systemId = d.getEntity().getSystemId();
        this.entityId = d.getEntity().getEntityId();
        this.entityQueryId = d.getEntity().getEntityQueryId();
        this.isMain = d.getEntity().getIsMain();
    }
}
