package ru.bssg.lottabyte.core.model.businessEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class BusinessEntity extends ModeledObject<BusinessEntityEntity> {

    public BusinessEntity() {
    }

    public BusinessEntity(BusinessEntityEntity entity) {
        super(entity);
    }

    public BusinessEntity(BusinessEntityEntity entity, Metadata md) {
        super(entity, md, ArtifactType.business_entity);
    }

}
