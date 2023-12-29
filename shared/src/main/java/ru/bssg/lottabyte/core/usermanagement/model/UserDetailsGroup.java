package ru.bssg.lottabyte.core.usermanagement.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserDetailsGroup {

    private Boolean addedSeparately;
    private ZonedDateTime createdAt;
    private String createdBy;
    private String description;
    private Long groupId;
    private UserDetailsMisc misc;
    private String name;
    private ZonedDateTime updatedAt;

}
