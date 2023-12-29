package ru.bssg.lottabyte.core.model.reference;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReferenceEntity extends Entity {

    private String sourceId;
    private ArtifactType sourceType;
    private String targetId;
    private String publishedId;
    private ArtifactType targetType;
    private ReferenceType referenceType;
    private Integer versionId;

    public ReferenceEntity() {
        super(ArtifactType.reference);
    }

}
