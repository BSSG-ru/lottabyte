package ru.bssg.lottabyte.core.model.ca;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class CustomAttributeDefinition extends ModeledObject<CustomAttributeDefinitionEntity> {

    public CustomAttributeDefinition() throws LottabyteException {
    }

    public CustomAttributeDefinition(CustomAttributeDefinitionEntity entity) throws LottabyteException {
        super(entity);
    }

    public CustomAttributeDefinition(CustomAttributeDefinitionEntity entity, Metadata md) {
        super(entity, md, ArtifactType.custom_attribute_definition);
    }

}
