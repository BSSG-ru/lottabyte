package ru.bssg.lottabyte.core.model.ca;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class CustomAttributeDefinitionEntity extends Entity {

    protected AttributeType type;
    protected Boolean multipleValues;
    protected String defaultValue;
    protected String placeholder;
    protected Integer minimum;
    protected Integer maximum;
    protected Integer minLength;
    protected Integer maxLength;
    protected Boolean required;
    protected List<CustomAttributeEnumValue> defElements;

    public CustomAttributeDefinitionEntity() {
        super(ArtifactType.custom_attribute_definition);
    }

}
