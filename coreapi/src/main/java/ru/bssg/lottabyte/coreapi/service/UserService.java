package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.externalGroup.ExternalGroup;
import ru.bssg.lottabyte.core.model.ldapProperty.LdapProperty;
import ru.bssg.lottabyte.core.usermanagement.model.Language;
import ru.bssg.lottabyte.core.usermanagement.model.UpdatableUserDetails;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.model.UserRole;
import ru.bssg.lottabyte.coreapi.repository.UserRepository;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserDetails createUser(UpdatableUserDetails newUserDetails, String tenant) throws LottabyteException {
        if (newUserDetails.getUsername() == null || newUserDetails.getUsername().isEmpty())
            throw new LottabyteException(Message.LBE00048, Language.ru);
        if (newUserDetails.getPassword() == null || newUserDetails.getPassword().isEmpty())
            throw new LottabyteException(Message.LBE00050, Language.ru);

        Integer id = userRepository.createUser(newUserDetails, tenant);
        return getUserById(id.toString(), tenant);
    }

    public UserDetails getUserById(String userId, String tenant) throws LottabyteException {
        UserDetails userDetails = userRepository.getPlatformUserById(userId, tenant);
        if (userDetails == null || userDetails.getUid() == null) {
            throw new LottabyteException(Message.LBE00046, userDetails == null ? Language.ru : userDetails.getLanguage(), userId);
        }
        return userDetails;
    }

    public List<UserDetails> getUsersByRoleName(String roleName, String tenant) {
        return userRepository.getUsersByRoleName(roleName, tenant);
    }

    public List<UserDetails> getUserByTenantId(String tenantId) {
        return userRepository.getPlatformUserByTenantId(tenantId);
    }

    public void deleteUserByTenant(String tenantId) {
        userRepository.deleteUserByTenant(tenantId);
    }

    public List<UserRole> getUserRolesByTenantId(String tenantId) {
        return userRepository.getUserRolesByTenantId(tenantId);
    }

    public UserRole getUserRoleById(String roleId, String tenantId) {
        return userRepository.getUserRoleById(roleId, tenantId);
    }

    public List<ExternalGroup> getExternalGroupsByTenantId(String tenantId) {
        return userRepository.getExternalGroupsByTenantId(tenantId);
    }

    public List<LdapProperty> getLdapPropertiesByTenantId(Integer tenantId) {
        return userRepository.getLdapPropertiesByTenantId(tenantId);
    }

    public void deleteUserRolesByTenantId(String tenantId) {
        userRepository.deleteUserRolesByTenantId(tenantId);
    }

    public void deleteExternalGroupsByTenantId(String tenantId) {
        userRepository.deleteExternalGroupsByTenantId(tenantId);
    }

    public void deleteLdapPropertiesByTenantId(Integer tenantId) {
        userRepository.deleteLdapPropertiesByTenantId(tenantId);
    }
}
