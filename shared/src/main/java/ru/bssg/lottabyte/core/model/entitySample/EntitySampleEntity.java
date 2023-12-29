package ru.bssg.lottabyte.core.model.entitySample;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class EntitySampleEntity extends Entity {

    private String entityId;
    private String systemId;
    private String entityQueryId;
    private Integer entityQueryVersionId;
    private Boolean isMain;
    private EntitySampleType sampleType;
    private List<EntitySampleDQRule> dqRules;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime lastUpdated;
    private String sampleBody;
    private String roles;

    public EntitySampleEntity() {
        super(ArtifactType.entity_sample);
    }

}
