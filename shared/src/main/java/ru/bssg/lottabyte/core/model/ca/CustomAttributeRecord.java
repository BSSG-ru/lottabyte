package ru.bssg.lottabyte.core.model.ca;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomAttributeRecord {

    private String id;
    private String customAttributeDefinitionId;

    private String objectId;
    private String objectType;
    private LocalDateTime dataValue;
    private Double numberValue;
    private String textValue;
    private String defElementId;

}
