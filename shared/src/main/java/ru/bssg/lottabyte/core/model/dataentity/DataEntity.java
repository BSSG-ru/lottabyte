package ru.bssg.lottabyte.core.model.dataentity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.entitySample.SearchableEntitySample;

import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper=false)
public class DataEntity extends ModeledObject<DataEntityEntity> {

    private List<String> domainIds;

    public DataEntity() {
    }

    public DataEntity(DataEntityEntity entity) {
        super(entity);
    }

    public DataEntity(DataEntityEntity entity, Metadata md) {
        super(entity, md, ArtifactType.entity);
    }

}
