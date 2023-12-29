package ru.bssg.lottabyte.core.model.businessEntity;

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
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper = true)
public class FlatBusinessEntity extends FlatModeledObject {
    private String techName;
    private String definition;
    private String regulation;
    private List<String> altNames;
    private ArtifactState state;
    private String workflowState;
    private String workflowTaskId;
    private List<FlatRelation> synonyms;
    private List<FlatRelation> beLinks;
    private String domainId;
    private List<String> tags;
    private String domainName;
    private String parentId;
    private String formula;
    private String examples;
    private String link;
    private String datatypeId;
    private String limits;
    private String roles;

    public FlatBusinessEntity(BusinessEntity s) {
        super(s.getFlatModeledObject());
        this.techName = s.getEntity().getTechName();
        this.definition = s.getEntity().getDefinition();
        this.regulation = s.getEntity().getRegulation();
        this.altNames = s.getEntity().getAltNames();
        this.domainId = s.getEntity().getDomainId();
        this.parentId = s.getEntity().getParentId();
        this.formula = s.getEntity().getFormula();
        this.examples = s.getEntity().getExamples();
        this.link = s.getEntity().getLink();
        this.datatypeId = s.getEntity().getDatatypeId();
        this.limits = s.getEntity().getLimits();
        this.roles = s.getEntity().getRoles();
    }
}
