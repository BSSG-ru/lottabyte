package ru.bssg.lottabyte.core.model.system;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class SystemConnectionEntity extends Entity {

    private String connectorId;
    private String systemId;
    private Boolean enabled;

    public SystemConnectionEntity() {
        super(ArtifactType.system_connection);
    }

}
