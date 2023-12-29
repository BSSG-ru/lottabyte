package ru.bssg.lottabyte.core.model.system;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

import java.util.stream.Collectors;

public class System extends ModeledObject<SystemEntity> {

    public System() {
    }

    public System(SystemEntity entity) {
        super(entity);
    }

    public System(SystemEntity entity, Metadata md) {
        super(entity, md, ArtifactType.system);
    }

}