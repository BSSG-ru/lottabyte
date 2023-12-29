package ru.bssg.lottabyte.coreapi.controller;

import io.swagger.v3.oas.annotations.Hidden;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.system.*;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.ui.model.SystemType;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.SystemService;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "System", description = "APIs for System.")
@RestController
@Slf4j
@RequestMapping("v1/systems")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class SystemController {
    private final SystemService systemService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets System by given guid.",
            description = "This method can be used to get System by given guid.",
            operationId = "get_system_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "System not found with given id"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{system_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"system_r"}, level = ANY_ROLE)
    public ResponseEntity<System> getSystemById(
            @Parameter(description = "Artifact ID of the System",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("system_id") String systemId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(systemService.getSystemById(systemId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets System version by given guid and version id.",
            description = "This method can be used to get System history version by given guid and version id.",
            operationId = "getSystemVersionById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System version has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "System version not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{system_id}/versions/{version_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"system_r"}, level = ANY_ROLE)
    public ResponseEntity<System> getSystemVersionById(
            @Parameter(description = "ID of the System", example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("system_id") String systemId,
            @Parameter(description = "Version ID of the System", example = "1")
            @PathVariable("version_id") Integer versionId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return ResponseEntity.ok(systemService.getSystemVersionById(systemId, versionId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets System versions list by domain guid.",
            description = "This method can be used to get System history versions by given guid.",
            operationId = "getSystemVersionsById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System versions have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{system_id}/versions", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"system_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<System>> getSystemVersionsById(
            @PathVariable("system_id") String systemId,
            @Parameter(description = "The maximum number of Systems to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        PaginatedArtifactList<System> list = systemService.getSystemVersions(
                systemId, offset, limit, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all Systems.",
            description = "This method can be used to get all all Systems.",
            operationId = "get_custom_attribute_definition_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Systems have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"system_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<System>> getAllPaginated(
            @Parameter(description = "The maximum number of Systems to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @Parameter(description = "Artifact state.")
            @RequestParam(value="state", defaultValue = "PUBLISHED") String artifactState,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        return new ResponseEntity<>(systemService.getSystemsPaginated(offset, limit, artifactState, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates System by given guid.",
            description = "This method can be used to create System by given guid.",
            operationId = "create_system"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/system", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"system_r", "system_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<System> createSystem(
            @RequestBody UpdatableSystemEntity newSystemEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return new ResponseEntity<>(systemService.createSystem(newSystemEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Updates System by given guid.",
            description = "This method can be used to update System by given guid.",
            operationId = "update_system"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System has been updates successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "System not found with given id"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{system_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"system_r", "system_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<System> patchSystem(
            @Parameter(description = "Artifact ID of the System",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("system_id") String systemId,
            @RequestBody UpdatableSystemEntity systemEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return new ResponseEntity<>(systemService.patchSystem(systemId, systemEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System has been deleted successfully for DRAFT domain. System draft marked for removal has been created for PUBLISHED system"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "System not found with given id"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{system_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Operation(
            summary = "Removes System by given guid.",
            description = "This method can be used to remove System by given guid.",
            operationId = "deleteSystem"
    )
    @Secured(roles = {"system_r", "system_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<?> deleteSystem(
            @Parameter(description = "Artifact ID of the System",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("system_id") String systemId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        System result = systemService.deleteSystem(systemId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        if (result == null) {
            ArchiveResponse resp = new ArchiveResponse();
            resp.setDeletedGuids(Collections.singletonList(systemId));
            return ResponseEntity.ok(resp);
        } else {
            return ResponseEntity.ok(result);
        }
    }

    // Folders

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets System Folder by given guid.",
            description = "This method can be used to get System Folder by given guid.",
            operationId = "get_system_folder_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System Folder has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "System folder not found with given id"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/folders/{folder_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"system_r"}, level = ANY_ROLE)
    public ResponseEntity<SystemFolder> getFolderById(
            @Parameter(description = "Artifact ID of the System Folder",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("folder_id") String folderId,
            @RequestParam(value="include_children", defaultValue = "true") Boolean includeChildren,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return new ResponseEntity<>(systemService.getSystemFolderById(folderId, includeChildren, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all root System Folders.",
            description = "This method can be used to get root System Folders by given guid.",
            operationId = "get_root_system_folders"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System Folders has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/folders/root", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"system_r"}, level = ANY_ROLE)
    public ResponseEntity<List<SystemFolder>> getRootFolders(
            @Parameter(description = "Include children",
                    example = "true")
            @RequestParam(value="include_children", defaultValue = "true") Boolean includeChildren,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
         return new ResponseEntity<>(systemService.getRootFolders(includeChildren, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates System Folder.",
            description = "This method can be used to create System Folder.",
            operationId = "create_system_folder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System Folder has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/folders", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"system_r", "system_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<SystemFolder> createFolder(
            @RequestBody UpdatableSystemFolderEntity newSystemFolderEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException
    {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);
        // 1. Ignore "children" attribute on body
        // 2. If parent_id is null, then add to with parent_id null (root folder)
        // 3. If parent_id not null - check parent_id existence
        // 4. Check no duplicate name inside same upper folder
        // 5. Check no circular links with folder will exist after adding new folder to DB!!!
        // 6. Create folder in DB
        // 7. Return system entity object
        return new ResponseEntity<>(systemService.createFolder(newSystemFolderEntity, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Updates System Folder by given guid.",
            description = "This method can be used to update System Folder by given guid.",
            operationId = "patch_system_folder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System Folder has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "System folder not found with given id"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/folders/{folder_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"system_r", "system_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<SystemFolder> patchFolder(
            @Parameter(description = "Artifact ID of the System Folder",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("folder_id") String folderId,
            @RequestBody UpdatableSystemFolderEntity systemFolderEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException
    {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);
        // 1. Ignore "children" attribute on body
        // 2. If parent_id is null, then folder is moved to root level
        // 3. If parent_id not null - check parent_id existence
        // 4. Check no duplicate name inside same upper folder
        // 5. Check no circular links with folder will exist after adding new folder to DB!!!
        // 6. We are updating ONLY filled fields in body, doing nothing with fields, that are null, except parent_id!!!
        // 7. Update folder in DB
        // 8. Return system entity object
        return new ResponseEntity<>(systemService.patchFolder(folderId, systemFolderEntity, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Deletes System Folder by given guid.",
            description = "This method can be used to delete System Folder by given guid.",
            operationId = "delete_system_folder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System Folder has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "System folder not found with given id"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/folders/{folder_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"system_r", "system_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<ArchiveResponse> deleteFolder(
            @Parameter(description = "Artifact ID of the System Folder",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("folder_id") String folderId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return new ResponseEntity<>(systemService.deleteFolder(folderId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all SystemsWithoutDomain.",
            description = "This method can be used to get all SystemsWithoutDomain.",
            operationId = "get_systems_without_domain_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SystemsWithoutDomain have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/unliked_to_domain/{domain_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"system_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<System>> getPaginatedSystemsWithoutDomain(
            @PathVariable("domain_id") String domainId,
            @Parameter(description = "The maximum number of Systems to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        return new ResponseEntity<>(systemService.getPaginatedSystemsWithoutDomain(domainId, offset, limit, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    // Search

    @Hidden
    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"system_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatSystem>> searchSystems(
            @RequestBody SearchRequestWithJoin request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        log.info(request.toString());

        SearchResponse<FlatSystem> res = systemService.searchSystems(request, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/types", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"system_r"}, level = ANY_ROLE)
    public ResponseEntity<List<SystemType>> getSystemTypes(
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return new ResponseEntity<>(systemService.getSystemTypes(jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }
}
