package ru.bssg.lottabyte.core.model.globalsearch;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class SearchableRuleEntity extends SearchableArtifactsEntity {
    private String ruleType;

}
