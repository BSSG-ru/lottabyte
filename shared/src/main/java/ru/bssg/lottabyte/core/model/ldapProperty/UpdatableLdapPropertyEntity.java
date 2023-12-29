package ru.bssg.lottabyte.core.model.ldapProperty;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;

import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        description = "Update ldap property entity"
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UpdatableLdapPropertyEntity extends LdapPropertyEntity {
    private String providerUrl;
    private String principal;
    private String credentials;
    private String base_dn;
    private String user_query;
    private Integer tenantId;

    public UpdatableLdapPropertyEntity(LdapPropertyEntity fromCopy) throws LottabyteException {
        this.setName(fromCopy.getName());
        this.setArtifactType(fromCopy.getArtifactType());
        this.setPrincipal(fromCopy.getPrincipal());
        this.setCredentials(fromCopy.getCredentials());
        this.setBase_dn(fromCopy.getBase_dn());
        this.setTenantId(fromCopy.getTenantId());
        this.setUser_query(fromCopy.getUser_query());
        this.setProviderUrl(fromCopy.getProviderUrl());
    }
}
