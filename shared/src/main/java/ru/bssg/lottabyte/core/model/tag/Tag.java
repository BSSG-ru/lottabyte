package ru.bssg.lottabyte.core.model.tag;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.system.SearchableSystem;

import java.util.stream.Collectors;

public class Tag extends ModeledObject<TagEntity> {

    public Tag() {
    }

    public Tag(TagEntity entity) {
        super(entity);
    }

    public Tag(TagEntity entity, Metadata md) {
        super(entity, md, ArtifactType.tag);
    }

}
