package ru.bssg.lottabyte.core.model.ca;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomAttributeEnumValue {

    private String id;
    private String name;
    private String description;

}
