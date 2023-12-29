package ru.bssg.lottabyte.core.model.entitySample;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.dataentity.SearchableDataEntity;

import java.util.stream.Collectors;

public class EntitySampleDQRule extends ModeledObject<EntitySampleDQRuleEntity> {

    public EntitySampleDQRule() throws LottabyteException {
    }

    public EntitySampleDQRule(EntitySampleDQRuleEntity entity) throws LottabyteException {
        super(entity);
    }

    public EntitySampleDQRule(EntitySampleDQRuleEntity entity, Metadata md) throws LottabyteException {
        super(entity, md, ArtifactType.entity_sample_to_dq_rule);
    }

}
