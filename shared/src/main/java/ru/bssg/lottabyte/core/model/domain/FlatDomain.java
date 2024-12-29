package ru.bssg.lottabyte.core.model.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.ArtifactState;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.FlatRelation;
import ru.bssg.lottabyte.core.model.IFlatModeledObjectWithTags;

import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=false)
public class FlatDomain extends FlatModeledObject implements IFlatModeledObjectWithTags {

    private List<FlatRelation> stewards;
    private ArtifactState state;
    private String workflowState;
    private String workflowTaskId;
    private List<String> tags;

    public FlatDomain(Domain d) {
        super(d.getFlatModeledObject());
    }

}
