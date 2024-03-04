package ru.bssg.lottabyte.core.model.businessEntity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BusinessEntityTreeNodeData {
    private String id;
    private String name;
    private String techName;
    private String domainName;
    private String altNames;
    private String synonyms;
    private String beLinks;
    private String modified;
    private String workflowState;
    private String tags;
}
