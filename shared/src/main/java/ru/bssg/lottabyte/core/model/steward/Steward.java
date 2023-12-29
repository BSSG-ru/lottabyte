package ru.bssg.lottabyte.core.model.steward;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.dataasset.SearchableDataAsset;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;

import java.util.stream.Collectors;

public class Steward extends ModeledObject<StewardEntity> {

    public Steward() throws LottabyteException {
    }

    public Steward(StewardEntity entity) throws LottabyteException {
        super(entity);
    }

    public Steward(StewardEntity entity, Metadata md) throws LottabyteException {
        super(entity, md, ArtifactType.steward);
    }

}
