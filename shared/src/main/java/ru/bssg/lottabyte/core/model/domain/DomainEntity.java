package ru.bssg.lottabyte.core.model.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.Entity;
import ru.bssg.lottabyte.core.model.ArtifactType;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class DomainEntity extends Entity {

    private List<String> stewards;
    private List<String> systemIds;

    public DomainEntity() {
        super(ArtifactType.domain);
    }

}
