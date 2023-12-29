package ru.bssg.lottabyte.core.model.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.dataasset.SearchableDataAsset;

import java.util.stream.Collectors;

public class Domain extends ModeledObject<DomainEntity> {

    public Domain() {
    }

    public Domain(DomainEntity entity) {
        super(entity);
    }

    public Domain(DomainEntity entity, Metadata md) {
        super(entity, md, ArtifactType.domain);
    }

}
