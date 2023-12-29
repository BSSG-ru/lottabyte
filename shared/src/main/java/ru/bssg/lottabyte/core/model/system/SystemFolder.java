package ru.bssg.lottabyte.core.model.system;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

import java.util.stream.Collectors;

public class SystemFolder extends ModeledObject<SystemFolderEntity> {

    public SystemFolder() {
    }

    public SystemFolder(SystemFolderEntity entity) {
        super(entity);
    }

    public SystemFolder(SystemFolderEntity entity, Metadata md) {
        super(entity, md, ArtifactType.system_folder);
    }

}
