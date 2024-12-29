package ru.bssg.lottabyte.core.ui.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class SearchColumn {

    public enum ColumnType { Text, Timestamp, Number, UUID, Array };

    public SearchColumn(String column, ColumnType columnType, String filterColumn) {
        this.column = column;
        this.columnType = columnType;
        this.filterColumn = filterColumn;
    }

    public SearchColumn(String column, ColumnType columnType) {
        this(column, columnType, null);
    }

    private String column;
    private ColumnType columnType;
    private String filterColumn;
}
