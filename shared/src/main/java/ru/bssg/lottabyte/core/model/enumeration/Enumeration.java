package ru.bssg.lottabyte.core.model.enumeration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

import java.util.stream.Collectors;

public class Enumeration extends ModeledObject<EnumerationEntity> {

    public Enumeration() {
    }

    public Enumeration(EnumerationEntity entity) {
        super(entity);
    }

    public Enumeration(EnumerationEntity entity, Metadata md) {
        super(entity, md, ArtifactType.enumeration);
    }

}
