package ru.bssg.lottabyte.core.ui.model.gojs;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.model.dataentity.DataEntityAttributeType;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityAttributeModelData {
    private String id;
    private String name;
    private DataEntityAttributeType attributeType;
}
