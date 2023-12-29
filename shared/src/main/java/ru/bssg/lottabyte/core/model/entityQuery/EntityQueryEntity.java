package ru.bssg.lottabyte.core.model.entityQuery;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

@Data
@EqualsAndHashCode(callSuper=false)
public class EntityQueryEntity extends Entity {

    private String queryText;
    private String entityId;
    private String systemId;

    public EntityQueryEntity() {
        super(ArtifactType.entity_query);
    }

}
