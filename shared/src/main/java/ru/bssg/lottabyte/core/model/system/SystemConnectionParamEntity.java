package ru.bssg.lottabyte.core.model.system;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class SystemConnectionParamEntity extends Entity {

    private String systemConnectionId;
    private String connectorParamId;
    private String paramValue;

    public SystemConnectionParamEntity() {
        super(ArtifactType.system_connection_param);
    }

}
