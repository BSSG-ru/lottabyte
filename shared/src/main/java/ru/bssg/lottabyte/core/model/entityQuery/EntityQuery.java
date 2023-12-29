package ru.bssg.lottabyte.core.model.entityQuery;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

import java.util.stream.Collectors;

public class EntityQuery extends ModeledObject<EntityQueryEntity> {

    public EntityQuery() {
    }

    public EntityQuery(EntityQueryEntity entity) {
        super(entity);
    }

    public EntityQuery(EntityQueryEntity entity, Metadata md) {
        super(entity, md, ArtifactType.entity_query);
    }

}
