package ru.bssg.lottabyte.core.model.businessEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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
