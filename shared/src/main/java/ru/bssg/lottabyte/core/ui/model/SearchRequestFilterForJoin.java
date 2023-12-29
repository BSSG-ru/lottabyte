package ru.bssg.lottabyte.core.ui.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class SearchRequestFilterForJoin {
    private String table;
    private String column;
    private String value;
    private String onColumn;
    private String equalColumn;
    private SearchRequestFilterOperator operator;
}
