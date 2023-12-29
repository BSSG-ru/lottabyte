package ru.bssg.lottabyte.core.model.globalsearch;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class GsiCustomAttributesDefinition {

    private long lastUpdatedAt;
    private String attributeName;
    private List<Object> attributeValue;

}
