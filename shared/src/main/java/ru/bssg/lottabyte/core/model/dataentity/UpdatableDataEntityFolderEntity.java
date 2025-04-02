package ru.bssg.lottabyte.core.model.dataentity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import ru.bssg.lottabyte.core.api.LottabyteException;

import java.util.ArrayList;

@Data
@EqualsAndHashCode(callSuper=false)
@ToString(callSuper = true)
public class UpdatableDataEntityFolderEntity extends DataEntityFolderEntity {

    public UpdatableDataEntityFolderEntity(DataEntityFolderEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setDescription(fromCopy.getDescription());
        this.setParentId(fromCopy.getParentId());
        if (fromCopy.getChildren() != null) {
            this.setChildren(new ArrayList<>(fromCopy.getChildren()));
        }
    }

    public UpdatableDataEntityFolderEntity() {

    }
}
