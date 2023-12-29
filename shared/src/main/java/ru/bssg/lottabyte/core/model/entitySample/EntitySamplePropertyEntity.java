package ru.bssg.lottabyte.core.model.entitySample;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class EntitySamplePropertyEntity extends Entity {

    private EntitySamplePropertyPathType pathType;
    private String path;
    private String entitySampleId;
    private String type;
    private List<String> mappedAttributeIds;

    public EntitySamplePropertyEntity() {
        super(ArtifactType.entity_sample_property);
    }

}
