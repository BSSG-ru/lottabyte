package ru.bssg.lottabyte.core.model.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;
import ru.bssg.lottabyte.core.model.relation.ParentRelation;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class SystemFolderEntity extends Entity {

    private String parentId;
    private List<ParentRelation> children;

    public SystemFolderEntity() {
        super(ArtifactType.system_folder);
    }

}
