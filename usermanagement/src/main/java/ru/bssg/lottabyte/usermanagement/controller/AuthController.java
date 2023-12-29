package ru.bssg.lottabyte.usermanagement.controller;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.token.BlackListToken;
import ru.bssg.lottabyte.core.model.token.Token;
import ru.bssg.lottabyte.core.model.token.UpdatableBlackListTokenEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.usermanagement.util.MessageConstants;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.usermanagement.entity.TenantLdapConfig;
import ru.bssg.lottabyte.usermanagement.payload.ApiResponseMessage;
import ru.bssg.lottabyte.usermanagement.payload.JwtAuthenticationResponse;
import ru.bssg.lottabyte.core.usermanagement.model.Language;
import ru.bssg.lottabyte.usermanagement.payload.LoginRequest;
import ru.bssg.lottabyte.usermanagement.service.TenantService;
import ru.bssg.lottabyte.usermanagement.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;


@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
@Tag(name = "AuthController", description = "APIs for AuthController.")
@Slf4j
@CrossOrigin(origins = {"${app.security.cors.origin}"})
@RestController
@RequestMapping("/v1/preauth")
public class AuthController {
    private final JwtHelper jwtHelper;
    private final UserService userService;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    @Lazy
    public AuthController(JwtHelper jwtHelper, PasswordEncoder passwordEncoder,
                          UserService userService, TenantService tenantService) {
        this.jwtHelper = jwtHelper;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.tenantService = tenantService;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @RequestMapping(value = "/validateAuth", method = RequestMethod.POST, produces = { "application/json"})
    @ApiResponses({
            @ApiResponse(responseCode = "401",description = "Unauthorized"),
            @ApiResponse(responseCode = "200",description = "Ok")
    })
    @Operation(
            summary = "authenticateUser",
            description = "This method can be used to authenticateUser.",
            operationId = "authenticate_user"
    )
    public ResponseEntity<?> authenticateUser(@RequestBody(required = false) LoginRequest loginRequest, HttpServletRequest httpRequest, @RequestHeader Map headers) {
    	if (loginRequest == null || loginRequest.getUsername().isEmpty() || loginRequest.getPassword().isEmpty()) {
		    log.info("Поиск username и password в header");
            loginRequest = new LoginRequest(headers.get("username") == null ? "" : headers.get("username").toString(), headers.get("username") == null ? "" : headers.get("password").toString(), headers.get("language") == null ? Language.en : Language.valueOf(headers.get("language").toString()));
    	}
    	if (loginRequest.getUsername().isEmpty() || loginRequest.getPassword().isEmpty()) {
		    log.info("Поиск username и password через basic аутентификацию");
            final String authorization = httpRequest.getHeader("Authorization");
            if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
                // Authorization: Basic base64credentials
                String base64Credentials = authorization.substring("Basic".length()).trim();
                byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                // credentials = username:password:language
                final String[] values = credentials.split(":", 3);
                loginRequest = new LoginRequest(values[0], values[1], values[2] == null ? Language.en : Language.valueOf(values[2]));
            }
    	}
    	if (loginRequest.getUsername().isEmpty() || loginRequest.getPassword().isEmpty()) {
		    log.error("Не хватает пользовательских данных(username or password)");
    		return new ResponseEntity(new ApiResponseMessage(false, MessageConstants.USERNAME_OR_PASSWORD_EMPTY), HttpStatus.BAD_REQUEST);
    	}
        log.info("Пользователь " + loginRequest.getUsername() + " проверяется в Базе");

        UserDetails userDetails = new UserDetails();

        String tenant = tenantService.getTenant(loginRequest.getUsername());
        try {
            userDetails = userService.getUserByUsername(loginRequest.getUsername(), tenant);
        } catch (UsernameNotFoundException e) {
            log.info("Пользователь " + loginRequest.getUsername() + " в базе не найден");
        }

        if (userDetails.getInternalUser() != null && userDetails.getInternalUser()) {
            if (!passwordEncoder.matches(loginRequest.getPassword(), userDetails.getPassword()))
                return new ResponseEntity(new ApiResponseMessage(false, MessageConstants.USERNAME_OR_PASSWORD_INVALID), HttpStatus.BAD_REQUEST);
        } else {
            log.info("Пользователь " + loginRequest.getUsername() + " проверяется в LDAP");
            TenantLdapConfig tenantLdapConfig = tenantService.getTenantLdapConfig(tenant);
            if (tenantLdapConfig == null)
                return new ResponseEntity<>(new ApiResponseMessage(false, MessageConstants.NO_LDAP_CONFIGURATION), HttpStatus.BAD_REQUEST);
            userDetails = authenticateLdap(loginRequest.getUsername(), loginRequest.getPassword(), tenantLdapConfig, tenant);
            if (userDetails == null)
                return new ResponseEntity<>(new ApiResponseMessage(false, MessageConstants.USER_NOT_FOUND_IN_LDAP), HttpStatus.BAD_REQUEST);
        }
        userDetails.setLanguage(loginRequest.getLanguage() == null ? Language.en : loginRequest.getLanguage());
        log.info("CLAIM " + userDetails.getUserDomains().size());
        String jwt = jwtHelper.createJwtForClaims(userDetails);
        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
    }

    @RequestMapping(value = "/createLongPeriodToken", method = RequestMethod.POST, produces = { "application/json"})
    @ApiResponses({
            @ApiResponse(responseCode = "401",description = "Unauthorized"),
            @ApiResponse(responseCode = "200",description = "Ok")
    })
    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "authenticateUser",
            description = "This method can be used to authenticateUser.",
            operationId = "authenticate_user"
    )
    @Secured(roles = {"global_admin"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<?> createLongPeriodToken(
            @RequestBody(required = false) UserDetails apiDataForLongPeriodToken,
            HttpServletRequest httpRequest,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);
        String jwt = jwtHelper.createLongPeriodToken(apiDataForLongPeriodToken, userDetails);
        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all Token.",
            description = "This method can be used to get Token by given guid.",
            operationId = "get_tokens_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Token with tenant not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/token/{tenant}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"global_admin"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<PaginatedArtifactList<Token>> getAllTokensPaginatedByTenant(
            @Parameter(description = "Tenant ID",
                    example = "1001")
            @PathVariable("tenant") String tenant,
            @Parameter(description = "The maximum number of Tokens to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers
    ) throws Exception {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(tenantService.getAllTokensPaginatedByTenant(tenant, offset, limit, userDetails), HttpStatus.OK);
    }
    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all Black List Tokens.",
            description = "This method can be used to get Black List Token by given guid.",
            operationId = "get_black_list_tokens_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Black List Token has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Black List Token with tenant not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/black_list_token", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"global_admin"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<PaginatedArtifactList<BlackListToken>> getAllBlackListTokensPaginated(
            @Parameter(description = "The maximum number of Black List Tokens to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers
    ) throws Exception {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(tenantService.getAllBlackListTokensPaginated(offset, limit, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Indicator by given guid.",
            description = "This method can be used to get Indicator by given guid.",
            operationId = "getIndicatorById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Indicator has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Indicator with specified id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/token/black_list_token/{token_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"global_admin"}, level = ANY_ROLE)
    public ResponseEntity<BlackListToken> getBlackListValueByTokenId(
            @Parameter(description = "Artifact ID of the token",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("token_id") String tokenId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(tenantService.getBlackListValueByTokenId(tokenId, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Indicator by given guid.",
            description = "This method can be used to get Indicator by given guid.",
            operationId = "getIndicatorById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Indicator has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Indicator with specified id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/black_list_token/{black_list_token_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"global_admin"}, level = ANY_ROLE)
    public ResponseEntity<BlackListToken> getBlackListValueById(
            @Parameter(description = "Artifact ID of the black_list_token_id",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("black_list_token_id") String blackListTokenId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(tenantService.getBlackListValueById(blackListTokenId, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates new black list token.",
            description = "This method can be used to create black list token.",
            operationId = "create_black_list_token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "black list token has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/black_list_token", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"global_admin"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<BlackListToken> createBlackListToken(
            @RequestBody UpdatableBlackListTokenEntity updatableBlackListTokenEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(tenantService.createBlackListToken(updatableBlackListTokenEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "delete black list token by given guid.",
            description = "This method can be used to update black list token.",
            operationId = "patch_black_list_token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "black list token has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "black list token with specified id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/black_list_token/{black_list_token_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"global_admin"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<BlackListToken> deleteBlackListTokenById(
            @Parameter(description = "Artifact ID of the black list token to be deleted",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("black_list_token_id") String blackListTokenId,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);
        tenantService.deleteBlackListTokenById(blackListTokenId, userDetails);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Operation(
            summary = "refreshToken",
            description = "This method can be used to refresh Token.",
            operationId = "refresh_token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "401",description = "Unauthorized"),
            @ApiResponse(responseCode = "200",description = "Ok")
    })
    @RequestMapping(value = "/refreshToken", method = RequestMethod.POST, produces = { "application/json"})
    public ResponseEntity<?> refreshToken(
            @RequestBody String token
    ) throws Exception {
        if (!token.isEmpty()) {
            String tenant = jwtHelper.getUserDataFromTokenWithExpiredToken(token, "tenant");
            String username = jwtHelper.getUserDataFromTokenWithExpiredToken(token, "username");
            try{
                UserDetails userDetails = userService.getUserByUsername(username, tenant);
                String jwt = jwtHelper.createJwtForClaims(userDetails);
                return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
            } catch(UsernameNotFoundException e){
                log.info("Пользователь " + jwtHelper.getUserFromToken(token) + " в базе не найден");
            }
        }
        return new ResponseEntity(new ApiResponseMessage(false, MessageConstants.USERNAME_OR_PASSWORD_INVALID), HttpStatus.BAD_REQUEST);
    }

    public UserDetails authenticateLdap(String username, String password, TenantLdapConfig tenantLdapConfig, String tenantId) {
        UserDetails userDetails;
        Boolean isAuthenticated;
        List<String> groups;
        List<Integer> groupIds = new ArrayList<>();
        Set<String> permissionSet = new HashSet<>();
        Set<String> roleSet = new HashSet<>();

        try {
            LdapContextSource s = new LdapContextSource();
            s.setUrl(tenantLdapConfig.getProviderUrl());
            s.setBase(tenantLdapConfig.getBaseDn());
            s.setUserDn(tenantLdapConfig.getPrincipal());
            s.setPassword(tenantLdapConfig.getCredentials());
            s.afterPropertiesSet();

            LdapTemplate template = new LdapTemplate(s);
            String filter = MessageFormat.format(tenantLdapConfig.getUserQuery(), username);
            log.info("LDAP filter is " + filter);
            isAuthenticated = template.authenticate("", filter, password);

            groups = template.search("",
                    MessageFormat.format(tenantLdapConfig.getUserQuery(), username),
                    (AttributesMapper<String>) attrs -> attrs.get("memberOf").get().toString());
            log.info("GROUPS: " + groups.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!isAuthenticated)
            return null;

        if (!groups.isEmpty()) {
            Map<String, Integer> allGroups = userService.getLdapGroups();
            for (String s : allGroups.keySet()) {
                if (groups.contains(s))
                    groupIds.add(allGroups.get(s));
            }
            if (!groupIds.isEmpty()) {
                permissionSet = userService.getPermissionsByGroups(groupIds);
                roleSet = userService.getRolesByGroups(groupIds);
                log.info("ROLESET: " + roleSet.toString());
                if (!roleSet.isEmpty())
                    permissionSet.addAll(userService.getPermissionsByRoles(roleSet.stream().collect(Collectors.toList())));
            }
        }

        try{
            userDetails = userService.getUserByUsername(username, tenantId);
        } catch (UsernameNotFoundException e){
            userService.insertUser(username, tenantId);
            userDetails = userService.getUserByUsername(username, tenantId);
        }
        
        userDetails.setPermissions(permissionSet.stream().collect(Collectors.toList()));
        return userDetails;
    }

    /*
    private Set<String> getAllGroupsRecursivelyByUserDistinguishedName(String dn, @Nullable String parentDN) {
        List<String> results = this.ldapTemplate.search(
                query().where("member").is(dn),
                (AttributesMapper<String>) attrs -> attrs.get("distinguishedName").get().toString()
        );

        for (String result : results) {
            if (!(result.equals(parentDN) //circular, ignore
                    || this.groups.contains(result) //duplicate, ignore
            )) {
                this.getAllGroupsRecursivelyByUserDistinguishedName(result, dn);
            }
        }
        this.groups.addAll(results);

        return this.groups;
    }*/

    /*List<String> distinguishedNames = template.search(
                query().where("objectCategory").is("user").and(
                        query().where("sAMAccountName").is(username)
                                .or(query().where("userPrincipalName").is(username))
                ),
                (AttributesMapper<String>) attrs -> attrs.get("distinguishedName").get().toString()
        );
        log.info("DNs list: " + distinguishedNames.toString());

        if (distinguishedNames.isEmpty()) {
            throw new UsernameNotFoundException("User not recognized in LDAP");
        }
    */

}
