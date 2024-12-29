package ru.bssg.lottabyte.core.ui.model.gojs;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.model.relation.Relation;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GojsModelLinkData {
    private String id;
    private String from;        //source_id
    private String to;          //target_id
    private String points;      //сериализованный массив json для фронта, определяет как рисуется линия связи
    private Integer zOrder;     //z-order для фронта
    private List<Relation> tags;
}
