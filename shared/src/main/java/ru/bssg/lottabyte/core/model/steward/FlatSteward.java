package ru.bssg.lottabyte.core.model.steward;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.FlatRelation;
import ru.bssg.lottabyte.core.model.relation.Relation;

import java.util.List;

@NoArgsConstructor
@Data
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=false)
public class FlatSteward extends FlatModeledObject {

    private Integer userId;
    private List<FlatRelation> domains;

    public FlatSteward(Steward s) {
        super(s.getFlatModeledObject());
        this.userId = s.getEntity().getUserId();
    }

}
