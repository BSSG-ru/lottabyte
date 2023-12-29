package ru.bssg.lottabyte.core.model.ca;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import ru.bssg.lottabyte.core.model.dataasset.SearchableDataAsset;
import ru.bssg.lottabyte.core.model.search.SearchableCustomAttribute;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Custom attribute with its values"
)
@Data
public class CustomAttribute {

    private String customAttributeDefinitionId;
    private String name;
    private List<CustomAttributeValue<?>> values;
    @JsonIgnore
    private AttributeType objectType;

    public CustomAttribute() {
    }

    public CustomAttribute(String customAttributeDefinitionId, String name) {
        this.customAttributeDefinitionId = customAttributeDefinitionId;
        this.name = name;
    }

    @Schema(
            description = "Custom attribute definition ID"
    )
    public String getCustomAttributeDefinitionId() {
        return this.customAttributeDefinitionId;
    }

    @Schema(
            description = "List of values"
    )
    public List<CustomAttributeValue<?>> getValues() {
        return this.values;
    }

    @Schema(
            description = "Custom Attribute name"
    )
    public String getName() {
        return this.name;
    }

    @Schema(
            hidden = true
    )
    @JsonIgnore
    public SearchableCustomAttribute getSearchableArtifact() {
        SearchableCustomAttribute sa = new SearchableCustomAttribute();

        sa.setAttributeName(this.getName());
        if (this.getObjectType().equals(AttributeType.String)) {

        } else if (this.getObjectType().equals(AttributeType.Enumerated)) {

        }
        //// FIX
        /*
        sa.setAttributeName(this.getName());

        if (this.getObjectType().equals(AttributeType.String)) {
            CustomAttributeValue<String> val = new CustomAttributeValue<>();
            //val.setValue(this.getTextValue());
            this.getValues().add(val);
        } else if (this.getObjectType().equals(AttributeType.Enumerated)) {
//            CustomAttributeValue<CustomAttributeEnumValue> val = new CustomAttributeValue<>();
//            CustomAttributeEnumValue enumValue = new CustomAttributeEnumValue();
            List<CustomAttributeValue<?>> customAttributeValueList = this.getValues();
            for(int i = 0; i < customAttributeValueList.size(); i++){
//                enumValue.setName(((CustomAttributeEnumValue) customAttributeValueList.get(i).getValue()).getName());
//                enumValue.setDescription(((CustomAttributeEnumValue) customAttributeValueList.get(i).getValue()).getDescription());
//                val.setValue(enumValue);
                sa.setAttributeValue(new ArrayList<>());
                sa.getAttributeValue().add(((CustomAttributeEnumValue) customAttributeValueList.get(i).getValue()).getName());
            }
        }*/
        return sa;
    }
}
