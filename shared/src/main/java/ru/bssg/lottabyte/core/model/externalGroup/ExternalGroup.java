package ru.bssg.lottabyte.core.model.externalGroup;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

import java.util.stream.Collectors;

public class ExternalGroup extends ModeledObject<ExternalGroupEntity> {

    public ExternalGroup() {
    }

    public ExternalGroup(ExternalGroupEntity entity) {
        super(entity);
    }

    public ExternalGroup(ExternalGroupEntity entity, Metadata md) {
        super(entity, md, ArtifactType.external_groups);
    }

}
