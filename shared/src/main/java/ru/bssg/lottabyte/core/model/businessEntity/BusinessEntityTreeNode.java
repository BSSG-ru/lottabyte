package ru.bssg.lottabyte.core.model.businessEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class BusinessEntityTreeNode {
    private String key;
    private BusinessEntityTreeNodeData data;
    private List<BusinessEntityTreeNode> children;
}
