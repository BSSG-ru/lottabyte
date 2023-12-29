package ru.bssg.lottabyte.core.model.externalGroup;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;

import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update external group entity"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UpdatableExternalGroupEntity extends ExternalGroupEntity {
    private List<String> permissions;
    private List<String> userRoles;
    private String attributes;
    private String tenant;

    public UpdatableExternalGroupEntity(ExternalGroupEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setAttributes(fromCopy.getAttributes());
        this.setTenant(fromCopy.getTenant());
        if (fromCopy.getPermissions() != null) {
            this.setPermissions(new ArrayList<>(fromCopy.getPermissions()));
        }
        if (fromCopy.getUserRoles() != null) {
            this.setUserRoles(new ArrayList<>(fromCopy.getUserRoles()));
        }
    }
}
