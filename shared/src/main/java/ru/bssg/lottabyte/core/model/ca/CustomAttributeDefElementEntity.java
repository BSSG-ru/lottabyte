package ru.bssg.lottabyte.core.model.ca;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class CustomAttributeDefElementEntity extends Entity {

    protected String definitionId;

    public CustomAttributeDefElementEntity() {
        super(ArtifactType.custom_attribute_defelement);
    }

}
