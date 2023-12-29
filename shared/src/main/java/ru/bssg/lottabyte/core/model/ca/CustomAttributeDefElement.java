package ru.bssg.lottabyte.core.model.ca;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class CustomAttributeDefElement extends ModeledObject<CustomAttributeDefElementEntity> {

    public CustomAttributeDefElement() throws LottabyteException {
    }

    public CustomAttributeDefElement(CustomAttributeDefElementEntity entity) throws LottabyteException {
        super(entity);
    }

    public CustomAttributeDefElement(CustomAttributeDefElementEntity entity, Metadata md) {
        super(entity, md, ArtifactType.custom_attribute_defelement);
    }

}
