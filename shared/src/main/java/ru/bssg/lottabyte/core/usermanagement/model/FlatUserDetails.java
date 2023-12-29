package ru.bssg.lottabyte.core.usermanagement.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.steward.Steward;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=false)
public class FlatUserDetails extends FlatModeledObject {
    private String username;
    private String displayName;
    private String email;
    private List<String> userRoles;
    private List<String> permissions;
    private String stewardId;

    public FlatUserDetails(UserDetails ud) {
        this.id = ud.getUid();
        this.username = ud.getUsername();
        this.displayName = ud.getDisplayName();
        this.email = ud.getEmail();
        this.stewardId = ud.getStewardId();
        if (ud.getUserRoles() != null)
            this.setUserRoles(new ArrayList<>(ud.getUserRoles()));
    }

}
