package ru.bssg.lottabyte.core.model.dqRule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.dataasset.SearchableDataAsset;

import java.util.stream.Collectors;

public class DQRule extends ModeledObject<DQRuleEntity> {

    public DQRule() {
    }

    public DQRule(DQRuleEntity entity) {
        super(entity);
    }

    public DQRule(DQRuleEntity entity, Metadata md) {
        super(entity, md, ArtifactType.dq_rule);
    }

}
