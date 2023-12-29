package ru.bssg.lottabyte.usermanagement.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.externalGroup.FlatExternalGroup;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.group.ExternalGroup;
import ru.bssg.lottabyte.core.usermanagement.model.group.UpdatableExternalGroupEntity;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.usermanagement.service.ExternalGroupService;
import ru.bssg.lottabyte.usermanagement.service.TenantService;
import ru.bssg.lottabyte.usermanagement.service.UserService;

import java.util.List;
import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;

@CrossOrigin(origins = { "${app.security.cors.origin}" })
@RestController
@RequestMapping("/v1/usermgmt")
public class UserManagementController {
        private final UserService userService;
        private final ExternalGroupService externalGroupService;
        private final TenantService tenantService;
        private final JwtHelper jwtHelper;

        @Autowired
        public UserManagementController(UserService userService, ExternalGroupService externalGroupService,
                        TenantService tenantService,
                        JwtHelper jwtHelper) {
                this.userService = userService;
                this.externalGroupService = externalGroupService;
                this.tenantService = tenantService;
                this.jwtHelper = jwtHelper;
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Ger group by id", description = "This method can be used to get group by id.", operationId = "getGroupById")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Group has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Group not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/user/group/{group_id}", method = RequestMethod.GET, produces = { "application/json" })
        @Secured
        public ResponseEntity<ExternalGroup> getExternalGroupById(
                        @PathVariable("group_id") String groupId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return ResponseEntity.ok(externalGroupService.getExternalGroupById(groupId, userDetails));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Create ExternalGroup", description = "This method can be used to create ExternalGroup.", operationId = "createExternalGroup")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "ExternalGroup has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/user/group", method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "admin" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<ExternalGroup> createExternalGroup(
                        @RequestBody UpdatableExternalGroupEntity updatableExternalGroupEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return ResponseEntity.ok(
                                externalGroupService.createExternalGroup(updatableExternalGroupEntity, userDetails));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Update user by id", description = "This method can be used to update User.", operationId = "updateUser")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "User has been updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "User not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/user/group/{group_id}", method = RequestMethod.PATCH, produces = {
                        "application/json" })
        @Secured(roles = { "admin" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<ExternalGroup> updateExternalGroup(
                        @PathVariable("group_id") String groupId,
                        @RequestBody UpdatableExternalGroupEntity updatableExternalGroupEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return ResponseEntity.ok(externalGroupService.updateExternalGroup(groupId, updatableExternalGroupEntity,
                                userDetails));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Delete ExternalGroup by id", description = "This method can be used to delete ExternalGroup.", operationId = "deleteExternalGroup")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "ExternalGroup has been deleted successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "ExternalGroup not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/user/group/{group_id}", method = RequestMethod.DELETE, produces = {
                        "application/json" })
        @Secured(roles = { "admin" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<ArchiveResponse> deleteExternalGroup(
                        @PathVariable("group_id") String groupId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return ResponseEntity.ok(externalGroupService.deleteExternalGroup(groupId, userDetails));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Ger user by id", description = "This method can be used to get User by id.", operationId = "getUserById")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "User has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "User not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/user/{user_id}", method = RequestMethod.GET, produces = { "application/json" })
        @Secured
        public ResponseEntity<UserDetails> getUserById(
                        @PathVariable("user_id") String userId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return ResponseEntity.ok(userService.getUserById(userId, userDetails.getTenant()));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Create user", description = "This method can be used to create User.", operationId = "createUser")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "User has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/user", method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "admin" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<UserDetails> createUser(
                        @RequestBody UpdatableUserDetails newUserDetails,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return ResponseEntity.ok(userService.createUser(newUserDetails, userDetails));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Update user by id", description = "This method can be used to update User.", operationId = "updateUser")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "User has been updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "User not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/user/{user_id}", method = RequestMethod.PATCH, produces = { "application/json" })
        @Secured(roles = { "admin" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<UserDetails> updateUser(
                        @PathVariable("user_id") String userId,
                        @RequestBody UpdatableUserDetails updatableUserDetails,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return ResponseEntity.ok(userService.updateUser(userId, updatableUserDetails, userDetails));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Delete user by id", description = "This method can be used to delete User.", operationId = "deleteUser")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "User has been deleted successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "User not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/user/{user_id}", method = RequestMethod.DELETE, produces = { "application/json" })
        @Secured(roles = { "admin" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<ArchiveResponse> deleteUser(
                        @PathVariable("user_id") String userId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return ResponseEntity.ok(userService.deleteUser(userId, userDetails));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Create new role", description = "This method can be used to create Role.", operationId = "createRole")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Role has been successfully created"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/role", method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "admin" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<UserRole> createRole(
                        @RequestBody UpdatableUserRole newUserRole,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                return ResponseEntity.ok(userService.createRole(newUserRole,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Patch role by id", description = "This method can be used to update User role by id.", operationId = "patchRoleById")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Role has been updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Role not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/role/{role_id}", method = RequestMethod.PATCH, produces = { "application/json" })
        @Secured(roles = { "admin" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<UserRole> patchRoleById(
                        @PathVariable("role_id") String roleId,
                        @RequestBody UpdatableUserRole userRole,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                return ResponseEntity.ok(userService.updateRole(roleId, userRole,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Delete role by id", description = "This method can be used to delete User Role by id.", operationId = "deleteRoleById")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Role has been deleted successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Role not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/role/{role_id}", method = RequestMethod.DELETE, produces = { "application/json" })
        @Secured(roles = { "admin" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<ArchiveResponse> deleteRoleById(
                        @PathVariable("role_id") String roleId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                return ResponseEntity.ok(
                                userService.deleteRole(roleId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get role by id", description = "This method can be used to get Role by id.", operationId = "getRoleById")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Role has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Role not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/role/{role_id}", method = RequestMethod.GET, produces = { "application/json" })
        @Secured
        public ResponseEntity<UserRole> getRoleById(
                        @PathVariable("role_id") String roleId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return ResponseEntity.ok(userService.getRoleById(roleId, userDetails));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Create user by name", description = "This method can be used to get User by name.", operationId = "getUserByUsername")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "User has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "User not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @CrossOrigin
        @RequestMapping(value = "/user/name/{user_name}", method = RequestMethod.GET, produces = { "application/json" })
        @Secured
        public ResponseEntity<UserDetails> getUserByUsername(
                        @PathVariable("user_name") String userName,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return ResponseEntity.ok(userService.getUserByUsername(userName, userDetails.getTenant()));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Update user password", description = "This method can be used to update user password.", operationId = "updatePassword")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "User has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "User not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @CrossOrigin
        @RequestMapping(value = "/user/updatePassword", method = RequestMethod.PATCH, produces = { "application/json" })
        @Secured
        public ResponseEntity<UserDetails> updatePassword(@RequestBody UpdatableUserPassword updatableUserPassword,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                return ResponseEntity.ok(userService.updateUserPassword(updatableUserPassword, userDetails));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get all roles", description = "This method can be used to get all user roles", operationId = "getAllRoles")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Roles have been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/roles", method = RequestMethod.GET, produces = { "application/json" })
        @Secured
        public ResponseEntity<List<UserRole>> getAllRoles(
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return ResponseEntity.ok(userService.getAllRoles(userDetails.getTenant()));
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get all permissions", description = "This method can be used to get all available permissions", operationId = "getAllPermissions")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Permissions have been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/permissions", method = RequestMethod.GET, produces = { "application/json" })
        @Secured
        public ResponseEntity<List<Permission>> getAllPermissions(
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return ResponseEntity.ok(userService.getAllPermission());
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get all permissions", description = "This method can be used to get all available permissions", operationId = "getAllPermissions")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Permissions have been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/permissions/{permission_id}", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured
        public ResponseEntity<Permission> getPermissionById(
                        @PathVariable("permission_id") String permissionId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return ResponseEntity.ok(userService.getPermissionById(permissionId, userDetails));
        }

        @Hidden
        @RequestMapping(value = "/user/search", method = RequestMethod.POST, produces = { "application/json" })
        @Secured
        public ResponseEntity<SearchResponse<FlatUserDetails>> searchUsers(
                        @RequestBody SearchRequestWithJoin request,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = HttpUtils.getToken(headers);
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                SearchResponse<FlatUserDetails> res = userService.searchUsers(request, userDetails);
                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Hidden
        @RequestMapping(value = "/permissions/search", method = RequestMethod.POST, produces = { "application/json" })
        @Secured
        public ResponseEntity<SearchResponse<FlatPermission>> searchPermissions(
                        @RequestBody SearchRequestWithJoin request,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = HttpUtils.getToken(headers);
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                SearchResponse<FlatPermission> res = userService.searchPermissions(request, userDetails);
                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Hidden
        @RequestMapping(value = "/external_group/search", method = RequestMethod.POST, produces = {
                        "application/json" })
        @Secured
        public ResponseEntity<SearchResponse<FlatExternalGroup>> searchExternalGroup(
                        @RequestBody SearchRequestWithJoin request,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = HttpUtils.getToken(headers);
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                SearchResponse<FlatExternalGroup> res = userService.searchExternalGroup(request, userDetails);
                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Hidden
        @RequestMapping(value = "/roles/search", method = RequestMethod.POST, produces = { "application/json" })
        @Secured
        public ResponseEntity<SearchResponse<FlatUserRole>> searchRoles(
                        @RequestBody SearchRequestWithJoin request,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = HttpUtils.getToken(headers);
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                SearchResponse<FlatUserRole> res = userService.searchRoles(request, userDetails);
                return new ResponseEntity<>(res, HttpStatus.OK);
        }
}
