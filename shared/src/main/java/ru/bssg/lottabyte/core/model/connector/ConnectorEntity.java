package ru.bssg.lottabyte.core.model.connector;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

@Data
@EqualsAndHashCode(callSuper=false)
public class ConnectorEntity extends Entity {

    private String systemType;
    private Boolean enabled;

    public ConnectorEntity() {
        super(ArtifactType.connector);
    }
}
