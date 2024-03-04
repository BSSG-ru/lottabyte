package ru.bssg.lottabyte.core.model.businessEntity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BusinessEntityTreeNode {
    private String key;
    private BusinessEntityTreeNodeData data;
    private List<BusinessEntityTreeNode> children;
}
