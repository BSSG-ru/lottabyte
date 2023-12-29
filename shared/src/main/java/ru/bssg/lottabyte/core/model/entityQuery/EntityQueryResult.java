package ru.bssg.lottabyte.core.model.entityQuery;

import lombok.Data;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleType;

@Data
public class EntityQueryResult {

    private EntitySampleType sampleType;
    private String textSampleBody;
    private byte[] binarySampleBody;

}
