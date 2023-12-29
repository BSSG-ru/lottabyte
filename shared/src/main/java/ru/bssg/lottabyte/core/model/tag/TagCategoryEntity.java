package ru.bssg.lottabyte.core.model.tag;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TagCategoryEntity extends Entity {

    private List<Tag> tags;

    public TagCategoryEntity() {
        super(ArtifactType.tag_category);
    }

}
