package ru.bssg.lottabyte.core.model.enumeration;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class EnumerationEntity extends Entity {
    private List<String> variants;

    public EnumerationEntity() {
        super(ArtifactType.enumeration);
    }
}
