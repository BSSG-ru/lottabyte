package ru.bssg.lottabyte.core.model.dataentity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.entitySample.SearchableEntitySampleProperty;

import java.util.List;
import java.util.stream.Collectors;

public class DataEntityAttribute extends ModeledObject<DataEntityAttributeEntity> {

    public DataEntityAttribute() {
    }

    public DataEntityAttribute(DataEntityAttributeEntity entity) {
        super(entity);
    }

    public DataEntityAttribute(DataEntityAttributeEntity entity, Metadata md) {
        super(entity, md, ArtifactType.entity_attribute);
    }

}
