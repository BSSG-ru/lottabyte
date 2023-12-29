package ru.bssg.lottabyte.core.usermanagement.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
@ToString(callSuper=true)
public class FlatUserRole extends FlatModeledObject {
    private List<String> permissions;

    public FlatUserRole(UserRole userRole) {
        this.id = userRole.getId();
        this.name = userRole.getName();
        this.description = userRole.getDescription();
        if (userRole.getPermissions() != null)
            this.setPermissions(new ArrayList<>(userRole.getPermissions()));
    }
}
