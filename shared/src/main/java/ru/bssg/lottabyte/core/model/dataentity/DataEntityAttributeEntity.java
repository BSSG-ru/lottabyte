package ru.bssg.lottabyte.core.model.dataentity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class DataEntityAttributeEntity extends Entity {

    private String entityId;
    private String enumerationId;
    private DataEntityAttributeType attributeType;
    private List<String> mappedSamplePropertyIds;
    private List<String> tags;
    private String attributeId;

    public DataEntityAttributeEntity() {
        super(ArtifactType.entity_attribute);
    }

}
