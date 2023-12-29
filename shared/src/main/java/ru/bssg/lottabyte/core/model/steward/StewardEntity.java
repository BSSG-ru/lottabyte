package ru.bssg.lottabyte.core.model.steward;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class StewardEntity extends Entity {

    protected Integer userId;
    protected List<String> domains;

    public StewardEntity() {
        super(ArtifactType.steward);
    }

}
