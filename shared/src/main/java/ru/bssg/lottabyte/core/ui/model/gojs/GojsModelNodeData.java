package ru.bssg.lottabyte.core.ui.model.gojs;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GojsModelNodeData {
    private String id;
    private String name;        //наименование
    private String type;        //тип нода на фронте, пока есть только один тип
    private String artifactType; //тип артефакта
    private String loc;         //строка с координатами для фронта
    private Integer zOrder;     //z-order для фронта
    private Boolean isGroup;    //true для сущности/таблицы, false для атрибута
    private String parentId;    //для атрибута id родительской сущности
    private String group;       //для атрибута то же самое что parentId, для сущности - пустая строка. Этот отдельный атрибут имел смысл в datazen, тут, возможно, он будет не нужен и можно будет удалить потом
    private String text;        //отображаемое наименование
    private Integer order;      //для атрибутов можно задать порядок отображения в списке в диаграмме
    private String datatype;    //тип атрибута
}
