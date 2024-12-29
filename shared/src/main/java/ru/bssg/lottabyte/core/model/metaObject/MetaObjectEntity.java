package ru.bssg.lottabyte.core.model.metaObject;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper=false)
public class MetaObjectEntity extends Entity {
    private String metaObjectType;
    private UUID parentId;
    private UUID metaDatabaseId;
    private Boolean isDeleted;
}
