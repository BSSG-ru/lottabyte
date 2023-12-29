package ru.bssg.lottabyte.core.model.dataentity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;
import ru.bssg.lottabyte.core.model.relation.ParentRelation;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class DataEntityFolderEntity extends Entity {

    private String parentId;
    private List<ParentRelation> children;

    public DataEntityFolderEntity() {
        super(ArtifactType.entity_folder);
    }

}
