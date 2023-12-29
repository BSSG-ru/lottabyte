package ru.bssg.lottabyte.core.ui.model;

import lombok.Data;

import java.util.List;

@Data
public class SearchSQLParts {
    private String where;
    private String join;
    private String orderBy;
    private List<Object> whereValues;
}
