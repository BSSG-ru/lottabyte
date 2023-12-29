package ru.bssg.lottabyte.core.model.dataasset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.ca.CustomAttribute;
import ru.bssg.lottabyte.core.model.dataentity.DataEntityEntity;
import ru.bssg.lottabyte.core.model.steward.SearchableSteward;

import java.util.List;
import java.util.stream.Collectors;

public class DataAsset extends ModeledObject<DataAssetEntity> {

    public DataAsset() {
        super(new DataAssetEntity());
    }

    public DataAsset(DataAssetEntity entity) {
        super(entity);
    }

    public DataAsset(DataAssetEntity entity, Metadata md) {
        super(entity, md, ArtifactType.data_asset);
    }

}