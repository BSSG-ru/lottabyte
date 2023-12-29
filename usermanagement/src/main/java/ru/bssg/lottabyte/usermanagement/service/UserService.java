package ru.bssg.lottabyte.usermanagement.service;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.externalGroup.FlatExternalGroup;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.*;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.usermanagement.repository.TenantRepostiory;
import ru.bssg.lottabyte.usermanagement.repository.UserRepository;

@Slf4j
@Service
public class UserService{
    private final UserRepository userRepository;
    private final TenantRepostiory tenantRepostiory;
    private final SearchColumn[] searchableColumnsForUser = {
            new SearchColumn("username", SearchColumn.ColumnType.Text),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            new SearchColumn("display_name", SearchColumn.ColumnType.Text),
            new SearchColumn("user_roles", SearchColumn.ColumnType.Array),
            new SearchColumn("user_roles.name", SearchColumn.ColumnType.Text)
    };
    private final SearchColumn[] searchableColumnsForExternalGroup = {
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            new SearchColumn("permissions", SearchColumn.ColumnType.Array),
            new SearchColumn("user_roles", SearchColumn.ColumnType.Array),
            new SearchColumn("attributes", SearchColumn.ColumnType.Array)
    };
    private final SearchColumn[] searchableColumnsForPermission = {
            new SearchColumn("id", SearchColumn.ColumnType.UUID),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            new SearchColumn("name", SearchColumn.ColumnType.Text)
    };

    private final SearchColumn[] userRoleSearchableColumns = {
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            new SearchColumn("permissions", SearchColumn.ColumnType.Array)
    };
    private final SearchColumnForJoin[] joinColumns = {
            new SearchColumnForJoin("name", "user_roles", SearchColumn.ColumnType.Text, "user_roles", "id")
    };

    @Autowired
    public UserService(UserRepository userRepository, TenantRepostiory tenantRepostiory) {
        this.userRepository = userRepository;
        this.tenantRepostiory = tenantRepostiory;
    }

    public Map<String, Integer> getLdapGroups(){
        return userRepository.getLdapGroups();
    }

    public Set<String> getPermissionsByGroups(List<Integer> groupIdList) {
        return userRepository.getPermissionsByGroups(groupIdList);
    }

    public Set<String> getRolesByGroups(List<Integer> groudIds) {
        return userRepository.getRolesByGroups(groudIds);
    }

    public Set<String> getPermissionsByRoles(List<String> roleIds) {
        return userRepository.getPermissionsByRoles(roleIds);
    }

    public UserDetails getUserByUsername(String username, String tenant) {
        String uname = username;
        if (uname != null && uname.contains("@"))
            uname = uname.split("@")[0];
        UserDetails userDetails = userRepository.getPlatformUserByUsername(uname, tenant);
        /*if (userDetails != null && userDetails.getInternalUser() != null && userDetails.getInternalUser()){
            userDetails = userRepository.getFullUserByUsername(username);
        }*/
        if (userDetails == null || userDetails.getUid() == null) {
            throw new UsernameNotFoundException("Unknown user: " + username);
        }
        return userDetails;
    }

    public UserDetails updateUserPassword(UpdatableUserPassword updatableUserPassword, UserDetails userDetails) throws LottabyteException {
        if (updatableUserPassword.getNewPassword() == null || updatableUserPassword.getNewPassword().isEmpty())
            throw new LottabyteException(Message.LBE00070, userDetails.getLanguage());
        if (updatableUserPassword.getOldPassword() == null || updatableUserPassword.getOldPassword().isEmpty())
            throw new LottabyteException(Message.LBE00050, userDetails.getLanguage());
        if (updatableUserPassword.getConfirmPassword() == null || !updatableUserPassword.getConfirmPassword().equals(updatableUserPassword.getNewPassword()))
            throw new LottabyteException(Message.LBE00071, userDetails.getLanguage());

        if (!userRepository.updateUserPassword(updatableUserPassword, userDetails))
            throw new LottabyteException(Message.LBE00072, userDetails.getLanguage());

        return userDetails;
    }

    public UserDetails getUserById(String userId, String tenant) throws LottabyteException {
        UserDetails userDetails = userRepository.getPlatformUserById(userId, tenant);
        /*if (userDetails != null && userDetails.getInternalUser() != null && userDetails.getInternalUser()){
            userDetails = userRepository.getFullUserByUsername(userDetails.getUsername());
        }*/
        if (userDetails == null || userDetails.getUid() == null) {
            throw new LottabyteException(Message.LBE00046, userDetails.getLanguage(), userId);
        }
        return userDetails;
    }

    public void insertUser(String username, String tenantId) {
        userRepository.insertUser(username, tenantId);
    }

    public List<UserRole> getAllRoles(String tenantId) {
        return userRepository.getAllRoles(tenantId);
    }

    public List<Permission> getAllPermission() {
        return userRepository.getAllPermissions();
    }
    public Permission getPermissionById(String permissionId, UserDetails userDetails) {
        return userRepository.getPermissionById(permissionId, userDetails);
    }

    public SearchResponse<FlatUserDetails> searchUsers(SearchRequestWithJoin request, UserDetails userDetails) throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumnsForUser, joinColumns, userDetails);
        return userRepository.searchUsers(request, searchableColumnsForUser, joinColumns, userDetails);
    }
    public SearchResponse<FlatPermission> searchPermissions(SearchRequestWithJoin request, UserDetails userDetails) throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumnsForPermission, joinColumns, userDetails);
        return userRepository.searchPermissions(request, searchableColumnsForPermission, joinColumns, userDetails);
    }
    public SearchResponse<FlatExternalGroup> searchExternalGroup(SearchRequestWithJoin request, UserDetails userDetails) throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumnsForExternalGroup, joinColumns, userDetails);
        return userRepository.searchExternalGroup(request, searchableColumnsForExternalGroup, joinColumns, userDetails);
    }

    public SearchResponse<FlatUserRole> searchRoles(SearchRequestWithJoin request, UserDetails userDetails) throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, userRoleSearchableColumns, joinColumns, userDetails);
        return userRepository.searchRoles(request, userRoleSearchableColumns, joinColumns, userDetails);
    }

    public UserDetails createUser(UpdatableUserDetails newUserDetails, UserDetails userDetails) throws LottabyteException {
        if (newUserDetails.getUsername() == null || newUserDetails.getUsername().isEmpty())
            throw new LottabyteException(Message.LBE00048, userDetails.getLanguage());
        if (userRepository.existsUserByUsername(newUserDetails.getUsername(), userDetails))
            throw new LottabyteException(Message.LBE00056, userDetails.getLanguage(), newUserDetails.getUsername());
        if (newUserDetails.getDisplayName() == null || newUserDetails.getDisplayName().isEmpty())
            throw new LottabyteException(Message.LBE00049, userDetails.getLanguage());
        if (newUserDetails.getPassword() == null || newUserDetails.getPassword().isEmpty())
            throw new LottabyteException(Message.LBE00050, userDetails.getLanguage());
        /*if ((newUserDetails.getPermissions() == null || newUserDetails.getPermissions().isEmpty()) &&
                (newUserDetails.getUserRolesIds() == null || newUserDetails.getUserRolesIds().isEmpty()))
            throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.LBE00057);*/
        validateUserRolesAndPermissions(newUserDetails, userDetails);
        Integer id = userRepository.createUser(newUserDetails, userDetails);
        return getUserById(id.toString(), userDetails.getTenant());
    }

    public UserDetails updateUser(String userId, UpdatableUserDetails updatableUserDetails, UserDetails userDetails) throws LottabyteException {
        if (!userRepository.existsUserById(userId, userDetails))
            throw new LottabyteException(Message.LBE00046, userDetails.getLanguage(), userId);
        UserDetails current = getUserById(userId, userDetails.getTenant());
        if (!current.getInternalUser())
            throw new LottabyteException(Message.LBE00058, userDetails.getLanguage(), userId);
        if (updatableUserDetails.getUsername() != null && updatableUserDetails.getUsername().isEmpty())
            throw new LottabyteException(Message.LBE00048, userDetails.getLanguage());
        if (updatableUserDetails.getUsername() != null && userRepository.existsUserByUsername(updatableUserDetails.getUsername(), userDetails))
            throw new LottabyteException(Message.LBE00056, userDetails.getLanguage(), updatableUserDetails.getUsername());
        if (updatableUserDetails.getDisplayName() != null && updatableUserDetails.getDisplayName().isEmpty())
            throw new LottabyteException(Message.LBE00049, userDetails.getLanguage());
        if (updatableUserDetails.getPassword() != null && updatableUserDetails.getPassword().isEmpty())
            throw new LottabyteException(Message.LBE00050, userDetails.getLanguage());
        /*if (updatableUserDetails.getPermissions() != null && updatableUserDetails.getPermissions().isEmpty() &&
                (current.getUserRoles() == null || current.getUserRoles().isEmpty()))
            throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.LBE00059);
        if (updatableUserDetails.getUserRolesIds() != null && updatableUserDetails.getUserRolesIds().isEmpty() &&
                (current.getPermissions() == null || current.getPermissions().isEmpty()))
            throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.LBE00060);*/
        validateUserRolesAndPermissions(updatableUserDetails, userDetails);
        userRepository.updateUser(userId, updatableUserDetails, userDetails);
        return getUserById(userId, userDetails.getTenant());
    }

    private void validateUserRolesAndPermissions(UpdatableUserDetails updatableUserDetails, UserDetails userDetails) throws LottabyteException {
        if (updatableUserDetails.getPermissions() != null) {
            for (String permission : updatableUserDetails.getPermissions()) {
                if (!userRepository.existsPermission(permission))
                    throw new LottabyteException(Message.LBE00054, userDetails.getLanguage(), permission);
            }
        }
        if (updatableUserDetails.getUserRolesIds() != null) {
            for (String roleId : updatableUserDetails.getUserRolesIds()) {
                if (!userRepository.existsRole(roleId, userDetails))
                    throw new LottabyteException(Message.LBE00061, userDetails.getLanguage(), roleId);
            }
        }
    }

    public ArchiveResponse deleteUser(String userId, UserDetails userDetails) throws LottabyteException {
        if (!userRepository.existsUserById(userId, userDetails))
            throw new LottabyteException(Message.LBE00046, userDetails.getLanguage(), userId);
        UserDetails current = getUserById(userId, userDetails.getTenant());
        if (!current.getInternalUser())
            throw new LottabyteException(Message.LBE00058, userDetails.getLanguage(), userId);

        userRepository.deleteUser(userId, userDetails);
        ArchiveResponse archiveResponse = new ArchiveResponse();
        archiveResponse.setArchivedGuids(Collections.singletonList(userId));
        return archiveResponse;
    }

    private void validateRolePermissions(List<String> permissions, UserDetails userDetails) throws LottabyteException {
        if (permissions != null) {
            for (String permission : permissions) {
                if (!userRepository.existsPermission(permission))
                    throw new LottabyteException(Message.LBE00054, userDetails.getLanguage(), permission);
            }
        }
    }

    public UserRole createRole(UpdatableUserRole newUserRole, UserDetails userDetails) throws LottabyteException {
        if (newUserRole.getName() == null || newUserRole.getName().isEmpty())
            throw new LottabyteException(Message.LBE00051, userDetails.getLanguage());
        if (userRepository.existsRoleByName(newUserRole.getName(), null, userDetails))
            throw new LottabyteException(Message.LBE00055, userDetails.getLanguage(), newUserRole.getName());
        //if (newUserRole.getPermissions() == null || newUserRole.getPermissions().isEmpty())
        //    throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.LBE00052);
        validateRolePermissions(newUserRole.getPermissions(), userDetails);
        String id = userRepository.createRole(newUserRole, userDetails);
        return userRepository.getRoleById(id, userDetails);
    }

    public UserRole updateRole(String roleId, UpdatableUserRole userRole, UserDetails userDetails) throws LottabyteException {
        if (!userRepository.existsRole(roleId, userDetails))
            throw new LottabyteException(Message.LBE00053, userDetails.getLanguage(), roleId);
        if (userRole.getName() != null && userRole.getName().isEmpty())
            throw new LottabyteException(Message.LBE00051, userDetails.getLanguage());
        if (userRole.getName() != null && userRepository.existsRoleByName(userRole.getName(), roleId, userDetails))
            throw new LottabyteException(Message.LBE00055, userDetails.getLanguage(), userRole.getName());
        //if (userRole.getPermissions() != null && userRole.getPermissions().isEmpty())
        //    throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.LBE00052);
        validateRolePermissions(userRole.getPermissions(), userDetails);
        userRepository.updateRole(roleId, userRole, userDetails);
        return userRepository.getRoleById(roleId, userDetails);
    }

    public ArchiveResponse deleteRole(String roleId, UserDetails userDetails) throws LottabyteException {
        if (!userRepository.existsRole(roleId, userDetails))
            throw new LottabyteException(Message.LBE00053, userDetails.getLanguage(), roleId);
        if (userRepository.existsUserWithRole(roleId, userDetails))
            throw new LottabyteException(Message.LBE00064, userDetails.getLanguage(), roleId);
        if (userRepository.existsExternalGroupWithRole(roleId, userDetails))
            throw new LottabyteException(Message.LBE00065, userDetails.getLanguage(), roleId);
        userRepository.deleteRole(roleId, userDetails);
        ArchiveResponse r = new ArchiveResponse();
        r.setArchivedGuids(Collections.singletonList(roleId));
        return r;
    }

    public UserRole getRoleById(String roleId, UserDetails userDetails) throws LottabyteException {
        return userRepository.getRoleById(roleId, userDetails);
    }

}
