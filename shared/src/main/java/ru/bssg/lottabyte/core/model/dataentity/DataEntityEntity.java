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

    public DataEntityEntity() {
        super(ArtifactType.entity);
    }

}
