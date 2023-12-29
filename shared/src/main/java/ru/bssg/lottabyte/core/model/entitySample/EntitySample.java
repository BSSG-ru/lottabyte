package ru.bssg.lottabyte.core.model.entitySample;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

import java.util.stream.Collectors;

public class EntitySample extends ModeledObject<EntitySampleEntity> {

    public EntitySample() {
    }

    public EntitySample(EntitySampleEntity entity) {
        super(entity);
    }
    public EntitySample(EntitySampleEntity entity, Metadata md) {
        super(entity, md, ArtifactType.entity_sample);
    }

}
