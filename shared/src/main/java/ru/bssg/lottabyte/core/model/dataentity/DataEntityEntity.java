package ru.bssg.lottabyte.core.model.dataentity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class DataEntityEntity extends Entity {

    private String entityFolderId;
    private List<String> systemIds;
    private String businessEntityId;
    private String roles;
    private String techName;

    public DataEntityEntity() {
        super(ArtifactType.entity);
    }

}
