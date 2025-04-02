package ru.bssg.lottabyte.core.model.etl;

import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class ETL extends ModeledObject<ETLEntity> {
    public ETL() {
    }

    public ETL(ETLEntity entity) {
        super(entity);
    }

    public ETL(ETLEntity entity, Metadata md) {
        super(entity, md, ArtifactType.etl);
    }
}
