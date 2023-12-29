package ru.bssg.lottabyte.core.ui.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class SearchColumnForJoin {
    public enum ColumnType { Text, Timestamp, Number, UUID };

    public SearchColumnForJoin(String column, String table, SearchColumn.ColumnType columnType, String onColumn, String equalColumn) {
        this.column = column;
        this.table = table;
        this.columnType = columnType;
        this.onColumn = onColumn;
        this.equalColumn = equalColumn;
    }

    private String table;
    private String column;
    private SearchColumn.ColumnType columnType;
    private String onColumn;
    private String equalColumn;
}
