package ru.bssg.lottabyte.core.model.etl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;
import ru.bssg.lottabyte.core.model.artifact.Artifact;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class ETLEntity extends Entity {
    private String algorithm;
    private String code;
    private String etlTypeId;
    private String systemId;
    private List<String> businessEntityIds;
    private List<Artifact> sourceIds;
    private List<Artifact> targetIds;

    public ETLEntity() {
        super(ArtifactType.etl);
    }
}
