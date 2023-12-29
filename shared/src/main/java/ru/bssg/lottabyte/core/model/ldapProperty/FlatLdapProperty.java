package ru.bssg.lottabyte.core.model.ldapProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.bssg.lottabyte.core.model.FlatModeledObject;

import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper = true)
public class FlatLdapProperty extends FlatModeledObject {
    private String providerUrl;
    private String principal;
    private String credentials;
    private String base_dn;
    private String user_query;
    private Integer tenantId;

    public FlatLdapProperty(LdapProperty s) {
        super(s.getFlatModeledObject());
        this.providerUrl = s.getEntity().getProviderUrl();
        this.principal = s.getEntity().getPrincipal();
        this.credentials = s.getEntity().getCredentials();
        this.base_dn = s.getEntity().getBase_dn();
        this.user_query = s.getEntity().getUser_query();
        this.tenantId = s.getEntity().getTenantId();
    }
}
