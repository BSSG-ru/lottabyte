package ru.bssg.lottabyte.core.model.system;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class SystemEntity extends Entity {

    private String systemType;
    private String connectorId;
    private String systemFolderId;
    private List<String> domainIds;

    public SystemEntity() {
        super(ArtifactType.system);
    }

}
