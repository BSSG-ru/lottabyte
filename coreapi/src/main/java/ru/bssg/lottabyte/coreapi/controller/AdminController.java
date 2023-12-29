package ru.bssg.lottabyte.coreapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.backupRun.BackupRun;
import ru.bssg.lottabyte.core.model.tenant.Tenant;
import ru.bssg.lottabyte.core.model.tenant.TenantValue;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.coreapi.service.AdminService;

import java.util.Map;
import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;

@CrossOrigin
@Tag(name = "Admin", description = "APIs for Admin")
@RestController
@Slf4j
@RequestMapping("v1/admin")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final JwtHelper jwtHelper;
    @Value("${system.data.storage}")
    private String DATA_STORAGE;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates new Tenant.",
            description = "This method can be used to create Tenant.",
            operationId = "create_tenant"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tenant has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/tenant", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"global_admin"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Tenant> createTenant(
            @RequestBody TenantValue tenantValue,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(adminService.createTenant(tenantValue, userDetails), HttpStatus.CREATED);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Delete Tenant.",
            description = "This method can be used to delete Tenant.",
            operationId = "create_tenant"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tenant has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/tenant/{tenant_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"global_admin"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<BackupRun> deleteTenant(
            @PathVariable("tenant_id") Integer tenantId,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);
        String tenantName = "da_" + tenantId;
        String path = DATA_STORAGE + "/" + tenantName + ".zip";
        BackupRun backupRun = adminService.createBackupRunBeforeRequest(tenantId, path);

        Thread deleteTenantThread = new Thread(() -> {
            try {
                adminService.deleteTenantWithCreatingBackup(tenantId, backupRun, userDetails);
            } catch (LottabyteException ignored) {}
        });
        deleteTenantThread.start();

        return new ResponseEntity<>(backupRun, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Create Backup For DB.",
            description = "This method can be used to createBackupForDB.",
            operationId = "create_backup"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "createBackupForDB has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/backup/{tenant_id}", method = RequestMethod.DELETE, produces = { "application/json"})
//    @Secured(roles = {"global_admin"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<BackupRun> createBackupForDB(
            @PathVariable("tenant_id") Integer tenantId,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String tenantName = "da_" + tenantId;
        String path = DATA_STORAGE + "/" + tenantName + ".zip";
        BackupRun backupRun = adminService.createBackupRunBeforeRequest(tenantId, path);

        return new ResponseEntity<>(backupRun, HttpStatus.OK);
    }
}
