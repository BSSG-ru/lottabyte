package ru.bssg.lottabyte.core.usermanagement.model.group;

import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

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
