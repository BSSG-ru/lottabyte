package ru.bssg.lottabyte.usermanagement.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TenantLdapConfig {

    private String providerUrl;
    private String principal;
    private String credentials;
    private String baseDn;
    private String userQuery;

}
