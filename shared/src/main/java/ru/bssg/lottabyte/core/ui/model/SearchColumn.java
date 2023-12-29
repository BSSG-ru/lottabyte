package ru.bssg.lottabyte.core.ui.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class SearchColumn {

    public enum ColumnType { Text, Timestamp, Number, UUID, Array };

    public SearchColumn(String column, ColumnType columnType) {
        this.column = column;
        this.columnType = columnType;
    }

    private String column;
    private ColumnType columnType;
}
