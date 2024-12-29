package ru.bssg.lottabyte.core.model.metaObject;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.IFlatModeledObjectWithTags;
import ru.bssg.lottabyte.core.model.tag.Tag;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper = true)
public class FlatMetaObject extends FlatModeledObject {
    private List<Tag> tags;
    private UUID parentId;
    private String parentName;
    private UUID metaDatabaseId;
    private String metaObjectType;

    public FlatMetaObject(MetaObject mo) {
        super(mo.getFlatModeledObject());
    }
}
