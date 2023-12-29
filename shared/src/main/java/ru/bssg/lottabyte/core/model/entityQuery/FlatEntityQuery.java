package ru.bssg.lottabyte.core.model.entityQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.ArtifactState;
import ru.bssg.lottabyte.core.model.FlatModeledObject;

import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=false)
public class FlatEntityQuery extends FlatModeledObject {
    private String systemId;
    private String entityId;
    private String systemName;
    private String entityName;
    private ArtifactState state;
    private String workflowState;
    private String workflowTaskId;
    private List<String> tags;

    public FlatEntityQuery(EntityQuery d) {
        super(d.getFlatModeledObject());
        this.systemId = d.getEntity().getSystemId();
        this.entityId = d.getEntity().getEntityId();
    }
}
