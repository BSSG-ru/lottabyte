package ru.bssg.lottabyte.core.usermanagement.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;

import java.util.ArrayList;

@NoArgsConstructor
@Data
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=false)
public class FlatPermission extends FlatModeledObject {
    private String id;
    private String name;
    private String description;

    public FlatPermission(Permission permission) {
        this.id = permission.getId();
        this.name = permission.getName();
        this.description = permission.getDescription();
    }

}
