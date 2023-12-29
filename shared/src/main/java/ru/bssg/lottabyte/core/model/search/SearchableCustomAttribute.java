package ru.bssg.lottabyte.core.model.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchableCustomAttribute {

    private String attributeName;
    private List<String> attributeValue;

}
