package ru.bssg.lottabyte.core.usermanagement.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserRole {

    private String id;
    private String name;
    private String description;
    private List<String> permissions;

}
