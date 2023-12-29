package ru.bssg.lottabyte.core.usermanagement.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@EqualsAndHashCode(callSuper=false)
public class UserDetailsExtended extends UserDetails {

    private Boolean isCustomPicture;
    private String profilePicture;


}
