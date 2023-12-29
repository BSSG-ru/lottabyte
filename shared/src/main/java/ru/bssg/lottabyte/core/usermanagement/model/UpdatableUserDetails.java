package ru.bssg.lottabyte.core.usermanagement.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdatableUserDetails {

    private String username;
    private String displayName;
    private String password;
    private String description;
    private String email;
    private List<String> permissions;
    private List<String> userRolesIds;
    private List<String> userDomains;

}
