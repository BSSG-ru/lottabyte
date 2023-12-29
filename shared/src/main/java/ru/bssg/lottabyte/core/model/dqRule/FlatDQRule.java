package ru.bssg.lottabyte.core.model.dqRule;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import ru.bssg.lottabyte.core.model.ArtifactState;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.FlatRelation;

import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class FlatDQRule extends FlatModeledObject {

    private ArtifactState state;
    private String ruleRef;
    private String settings;
    private String workflowState;
    private String workflowTaskId;
    private List<String> tags;
    private String ruleTypeId;
    private String ruleTypeName;

    public FlatDQRule(DQRule d) {
        super(d.getFlatModeledObject());
        this.ruleRef = d.getEntity().getRuleRef();
        this.settings = d.getEntity().getSettings();
    }

}
