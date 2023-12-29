package ru.bssg.lottabyte.core.model.datatype;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
public class DataTypeEntity extends Entity {
    public DataTypeEntity() {
        super(ArtifactType.datatype);
    }
}
