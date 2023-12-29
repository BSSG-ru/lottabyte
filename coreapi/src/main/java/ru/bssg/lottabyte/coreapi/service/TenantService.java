package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.model.tenant.Tenant;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.TenantRepository;
import ru.bssg.lottabyte.coreapi.repository.VersionRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class TenantService {
    private final TenantRepository tenantRepository;

    public Tenant getTenantByName(String name) {
        return tenantRepository.getTenantByName(name);
    }
    public Tenant getTenantById(Integer id) {
        return tenantRepository.getTenantById(id);
    }
    public Tenant getTenantByDomain(String domain) {
        return tenantRepository.getTenantByDomain(domain);
    }
    public Integer createTenant(String tenantName, String domainName, UserDetails userDetails) {
        return tenantRepository.createTenant(tenantName, domainName, userDetails);
    }
    public void deleteTenant(Integer id) {
        tenantRepository.deleteTenant(id);
    }
}
