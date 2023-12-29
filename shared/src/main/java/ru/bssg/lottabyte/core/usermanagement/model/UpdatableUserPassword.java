package ru.bssg.lottabyte.core.usermanagement.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdatableUserPassword {
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
}
