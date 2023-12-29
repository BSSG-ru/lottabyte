package ru.bssg.lottabyte.core.model.tenant;

import lombok.Data;

@Data
public class TenantValue {
    private String tenantName;
    private String domainName;
    private String password;
}
