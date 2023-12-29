package ru.bssg.lottabyte.core.model.indicator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRule;
import ru.bssg.lottabyte.core.model.relation.Relation;
import ru.bssg.lottabyte.core.model.tag.SearchableTag;

import java.util.List;
import java.util.stream.Collectors;

public class Indicator extends ModeledObject<IndicatorEntity> {

    public Indicator() {
    }

    public Indicator(IndicatorEntity entity) {
        super(entity);
    }

    public Indicator(IndicatorEntity entity, Metadata md) {
        super(entity, md, ArtifactType.indicator);
    }

}
