package ru.bssg.lottabyte.core.model.tag;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.steward.StewardEntity;

public class TagCategory extends ModeledObject<TagCategoryEntity> {

    public TagCategory() throws LottabyteException {
    }

    public TagCategory(TagCategoryEntity entity) throws LottabyteException {
        super(entity);
    }

    public TagCategory(TagCategoryEntity entity, Metadata md) throws LottabyteException {
        super(entity, md, ArtifactType.tag_category);
    }
}
