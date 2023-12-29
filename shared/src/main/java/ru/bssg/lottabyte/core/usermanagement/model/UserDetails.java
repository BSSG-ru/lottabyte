package ru.bssg.lottabyte.core.usermanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@EqualsAndHashCode(callSuper=false)
public class UserDetails extends ApiDataForLongPeriodToken {

    private String approvalStatus;
    @JsonIgnore
    private String authenticator;
    private Long createdTimestamp;
    private String currentAccountStatus;
    private Boolean deletable;
    private String displayName;
    private String email;
    private List<String> groupRoles;
    private List<UserDetailsGroup> groups;
    private Boolean internalUser;
    private Long lastModifiedTimestamp;
    private UserDetailsMisc misc;
    private List<String> permissions;
    private String role;
    private String uid;
    private List<String> userRoles;
    private List<UUID> userDomains;
    private String username;
    private String stewardId;
    @JsonIgnore
    private String password;
    private String tenant;
    private Language language;

    public UserDetails() {
        this.groupRoles = new ArrayList<String>();
        this.permissions = new ArrayList<String>();
        this.userRoles = new ArrayList<String>();
        this.groups = new ArrayList<UserDetailsGroup>();
        this.language = Language.en;
    }
    
}
