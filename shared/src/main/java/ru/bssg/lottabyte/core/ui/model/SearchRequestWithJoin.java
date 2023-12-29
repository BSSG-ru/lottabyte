package ru.bssg.lottabyte.core.ui.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class SearchRequestWithJoin {
    private String sort;
    private String globalQuery;
    private Integer limit;
    private Integer offset;
    private Boolean limitSteward;
    private List<SearchRequestFilter> filters;
    private List<SearchRequestFilterForJoin> filtersForJoin;
    private String state;
}
