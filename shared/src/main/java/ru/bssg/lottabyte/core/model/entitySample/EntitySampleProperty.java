package ru.bssg.lottabyte.core.model.entitySample;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.dataentity.SearchableDataEntity;

import java.util.stream.Collectors;

public class EntitySampleProperty extends ModeledObject<EntitySamplePropertyEntity> {

    public EntitySampleProperty() throws LottabyteException {
    }

    public EntitySampleProperty(EntitySamplePropertyEntity entity) throws LottabyteException {
        super(entity);
    }

    public EntitySampleProperty(EntitySamplePropertyEntity entity, Metadata md) throws LottabyteException {
        super(entity, md, ArtifactType.entity_sample_property);
    }

}
