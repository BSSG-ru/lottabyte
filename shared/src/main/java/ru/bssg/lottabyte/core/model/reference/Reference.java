package ru.bssg.lottabyte.core.model.reference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class Reference extends ModeledObject<ReferenceEntity> {

    public Reference() {
    }

    public Reference(ReferenceEntity entity) {
        super(entity);
    }

    public Reference(ReferenceEntity entity, Metadata md) {
        super(entity, md, ArtifactType.reference);
    }

}
