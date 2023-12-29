package ru.bssg.lottabyte.usermanagement.repository;

import ru.bssg.lottabyte.core.model.externalGroup.FlatExternalGroup;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserRepository {
    Map<String, Integer> getLdapGroups();
    Set<String> getRolesByGroups(List<Integer> groupIds);
    Set<String> getPermissionsByGroups(List<Integer> groupIdList);
    Set<String> getPermissionsByRoles(List<String> roleIds);
    List<UserRole> getAllRoles(String tenant);
    List<Permission> getAllPermissions();
    Permission getPermissionById(String permissionId, UserDetails userDetails);
    UserDetails getPlatformUserByUsername(String username, String tenant);
    UserDetails getPlatformUserById(String userId, String tenant);
    boolean updateUserPassword(UpdatableUserPassword updatableUserPassword, UserDetails userDetails);
    UserDetails getFullUserByUsername(String username);
    SearchResponse<FlatUserDetails> searchUsers(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails);
    SearchResponse<FlatPermission> searchPermissions(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails);
    SearchResponse<FlatExternalGroup> searchExternalGroup(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails);
    SearchResponse<FlatUserRole> searchRoles(SearchRequestWithJoin searchRequest, SearchColumn[] searchableColumns, SearchColumnForJoin[] searchableColumnsForJoin, UserDetails userDetails);
    void insertUser(String username, String tenantId);
    Integer createUser(UpdatableUserDetails newUserDetails, UserDetails userDetails);
    Boolean existsRole(String roleId, UserDetails userDetails);
    Boolean existsRoleByName(String roleName, String currentId, UserDetails userDetails);
    Boolean existsPermission(String permission);
    Boolean existsUserByUsername(String username, UserDetails userDetails);
    Boolean existsUserById(String uid, UserDetails userDetails);
    Boolean existsUserWithRole(String roleId, UserDetails userDetails);
    Boolean existsExternalGroupWithRole(String roleId, UserDetails userDetails);
    String createRole(UpdatableUserRole newUserRole, UserDetails userDetails);
    void updateRole(String roleId, UpdatableUserRole userRole, UserDetails userDetails);
    UserRole getRoleById(String roleId, UserDetails userDetails);
    void deleteRole(String roleId, UserDetails userDetails);
    void updateUser(String userId, UpdatableUserDetails updatableUserDetails, UserDetails ud);
    void deleteUser(String userId, UserDetails userDetails);
}
