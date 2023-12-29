package ru.bssg.lottabyte.core.model.ca;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(
        description = "Custom Attribute Value"
)
@Data
public class CustomAttributeValue<V> {

    @Schema(
            description = "Value"
    )
    private V value;

    @JsonIgnore
    private String customAttributeId;

    public CustomAttributeValue() {
    }

}
