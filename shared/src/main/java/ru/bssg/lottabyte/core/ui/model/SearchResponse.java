package ru.bssg.lottabyte.core.ui.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.businessEntity.FlatBusinessEntity;

import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class SearchResponse<T extends FlatModeledObject> {

    private Integer count;
    private Integer limit;
    private Integer offset;
    List<T> items;

    public SearchResponse() {
        items = new ArrayList<>();
    }

    public SearchResponse(Integer count, Integer limit, Integer offset, List<T> items) {
        this.count = count;
        this.limit = limit;
        this.offset = offset;

        int num = offset + 1;
        for (T item : items)
            item.setNum(num++);

        this.items = items;
    }

}
