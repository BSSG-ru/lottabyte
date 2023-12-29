package ru.bssg.lottabyte.core.model.ldapProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class LdapPropertyEntity extends Entity {
    private String providerUrl;
    private String principal;
    private String credentials;
    private String base_dn;
    private String user_query;
    private Integer tenantId;

    public LdapPropertyEntity() {
        super(ArtifactType.ldap_properties);
    }
}
