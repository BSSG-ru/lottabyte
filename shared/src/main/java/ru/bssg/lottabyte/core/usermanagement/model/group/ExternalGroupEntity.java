package ru.bssg.lottabyte.core.usermanagement.model.group;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExternalGroupEntity extends Entity {
    private List<String> permissions;
    private List<String> userRoles;
    private String attributes;
    private String tenant;

    public ExternalGroupEntity() {
        super(ArtifactType.external_groups);
    }

}
