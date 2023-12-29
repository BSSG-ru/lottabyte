package ru.bssg.lottabyte.core.model.indicator;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.ArtifactState;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.system.System;

import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper = true)
public class FlatIndicator extends FlatModeledObject {
    private List<String> dataAssetIds;
    private String dataAssetName;
    private String formula;
    private String calcCode;
    private List<String> dqChecks;
    private ArtifactState state;
    private String workflowState;
    private String workflowTaskId;
    private String domainId;
    private String indicatorTypeId;
    private String domainName;
    private String indicatorTypeName;
    private List<String> tags;
    private String examples;
    private String link;
    private String datatypeId;
    private String limits;
    private String limitsInternal;
    private String roles;

    public FlatIndicator(Indicator s) {
        super(s.getFlatModeledObject());
        this.dataAssetIds = s.getEntity().getDataAssetIds();
        this.calcCode = s.getEntity().getCalcCode();
        this.dqChecks = s.getEntity().getDqChecks();
        this.formula = s.getEntity().getFormula();
        this.domainId = s.getEntity().getDomainId();
        this.indicatorTypeId = s.getEntity().getIndicatorTypeId();
        this.examples = s.getEntity().getExamples();
        this.link = s.getEntity().getLink();
        this.datatypeId = s.getEntity().getDatatypeId();
        this.limits = s.getEntity().getLimits();
        this.limitsInternal = s.getEntity().getLimits_internal();
        this.roles = s.getEntity().getRoles();
    }
}
