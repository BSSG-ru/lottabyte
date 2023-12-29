package ru.bssg.lottabyte.core.model.tenant;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

@Data
@EqualsAndHashCode(callSuper=false)
public class TenantEntity extends Entity {
    private String domain;
    private Boolean defaultTenant;

    public TenantEntity() {
        super(ArtifactType.tenant);
    }

}
