package ru.bssg.lottabyte.core.model.system;

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
@EqualsAndHashCode(callSuper=false)
public class FlatSystem extends FlatModeledObject {

    private List<FlatRelation> domains;
    private ArtifactState state;
    private String workflowState;
    private String workflowTaskId;
    private List<String> tags;

    public FlatSystem(System s) {
        super(s.getFlatModeledObject());
    }

}
