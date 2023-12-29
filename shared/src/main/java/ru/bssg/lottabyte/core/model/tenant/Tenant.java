package ru.bssg.lottabyte.core.model.tenant;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Metadata;
import ru.bssg.lottabyte.core.model.ModeledObject;

public class Tenant extends ModeledObject<TenantEntity> {

    public Tenant() throws LottabyteException {
    }

    public Tenant(TenantEntity entity) throws LottabyteException {
        super(entity);
    }

    public Tenant(TenantEntity entity, Metadata md) throws LottabyteException {
        super(entity, md, ArtifactType.tenant);
    }

}
