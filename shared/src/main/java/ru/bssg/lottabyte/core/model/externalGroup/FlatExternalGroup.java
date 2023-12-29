package ru.bssg.lottabyte.core.model.externalGroup;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;

import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper = true)
public class FlatExternalGroup extends FlatModeledObject {
    private List<String> permissions;
    private List<String> userRoles;
    private String attributes;
    private String tenant;

    public FlatExternalGroup(ExternalGroup s) {
        super(s.getFlatModeledObject());
        this.permissions = s.getEntity().getPermissions();
        this.userRoles = s.getEntity().getUserRoles();
        this.attributes = s.getEntity().getAttributes();
        this.tenant = s.getEntity().getTenant();
    }
}
