package ru.bssg.lottabyte.coreapi.controller;

import com.amazonaws.services.xray.model.Http;
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
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.dataentity.*;
import ru.bssg.lottabyte.core.model.entitySample.FlatEntitySample;
import ru.bssg.lottabyte.core.ui.model.EntityAttributeType;
import ru.bssg.lottabyte.core.ui.model.SearchRequest;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.ui.model.gojs.EntityModelData;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelData;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelNodeData;
import ru.bssg.lottabyte.core.ui.model.gojs.UpdatableGojsModelData;
import ru.bssg.lottabyte.core.usermanagement.model.Language;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.EntityService;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;

import java.util.Collections;
import java.util.List;
import java.util.*;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "Entity", description = "APIs for Entities.")
@RestController
@Slf4j
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequestMapping("v1/entities")
@RequiredArgsConstructor
public class EntityController {
    private final EntityService entityService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Entity by given guid.",
            description = "This method can be used to get Steward by given guid.",
            operationId = "get_entity_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Steward has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Steward with steward_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{entity_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"lo_r"}, level = ANY_ROLE)
    public ResponseEntity<DataEntity> getEntityById(
            @Parameter(description = "Artifact ID of the Entity",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("entity_id") String entityId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(entityService.getDataEntityById(entityId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all DataEntity.",
            description = "This method can be used to get all DataEntity.",
            operationId = "get_entities_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataEntity has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "DataEntities not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"lo_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<DataEntity>> getAllEntitiesPaginated(
            @Parameter(description = "The maximum number of Stewards to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @Parameter(description = "Artifact state.")
            @RequestParam(value="state", defaultValue = "PUBLISHED") String artifactState,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(entityService.getAllEntitiesPaginated(offset, limit, artifactState, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Entity versions list by domain guid.",
            description = "This method can be used to get Entity history versions by given guid.",
            operationId = "getEntityVersionsById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity versions have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{entity_id}/versions", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"lo_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<DataEntity>> getEntityVersionsById(
            @PathVariable("entity_id") String entityId,
            @Parameter(description = "The maximum number of Stewards to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        PaginatedArtifactList<DataEntity> list = entityService.getEntityVersions(
                entityId, offset, limit, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Entity version by given guid and version id.",
            description = "This method can be used to get Entity history version by given guid and version id.",
            operationId = "getEntityVersionById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity version has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Entity version not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{entity_id}/versions/{version_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"lo_r"}, level = ANY_ROLE)
    public ResponseEntity<DataEntity> getEntityVersionById(
            @Parameter(description = "ID of the Entity", example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("entity_id") String entityId,
            @Parameter(description = "Version ID of the Domain", example = "1")
            @PathVariable("version_id") Integer versionId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return ResponseEntity.ok(entityService.getEntityVersionById(entityId, versionId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates DataEntity.",
            description = "This method can be used to creating DataEntity.",
            operationId = "create_entity"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataEntity has been added successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"lo_r", "lo_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<DataEntity> createEntity(@RequestBody UpdatableDataEntityEntity newDataEntityEntity,
                                                   @RequestHeader HttpHeaders headers) throws LottabyteException {
        return new ResponseEntity<>(entityService.createDataEntity(newDataEntityEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Update DataEntity.",
            description = "This method can be used to updating DataEntity.",
            operationId = "update_entity"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataEntity has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{entity_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"lo_r", "lo_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<DataEntity> patchEntity(
            @Parameter(description = "Artifact ID of the Entity",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("entity_id") String entityId,
            @RequestBody UpdatableDataEntityEntity dataEntityEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return new ResponseEntity<>(entityService.updateDataEntity(entityId, dataEntityEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Delete DataEntity.",
            description = "This method can be used to deleting DataEntity.",
            operationId = "delete_entity"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataEntity has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "DataEntity with entity_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{entity_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"lo_r", "lo_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<?> deleteEntity(
            @Parameter(description = "Artifact ID of the Entity",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("entity_id") String entityId,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        DataEntity result = entityService.deleteDataEntityById(entityId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        if (result == null) {
            ArchiveResponse resp = new ArchiveResponse();
            resp.setDeletedGuids(Collections.singletonList(entityId));
            return ResponseEntity.ok(resp);
        } else {
            return ResponseEntity.ok(result);
        }
    }

    // Attributes

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets DataEntityAttribute by given guid.",
            description = "This method can be used to get DataEntityAttribute by given guid.",
            operationId = "get_entity_attribute_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataEntityAttribute has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "DataEntityAttribute with entity_attribute_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/attributes/{entity_attribute_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"lo_r"}, level = ANY_ROLE)
    public ResponseEntity<DataEntityAttribute> getEntityAttributeById(
            @Parameter(description = "Artifact ID of the Entity Attribute",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("entity_attribute_id") String entityAttributeId,
            @RequestHeader HttpHeaders headers) throws Exception {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(entityService.getEntityAttributeById(entityAttributeId, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all DataEntityAttribute.",
            description = "This method can be used to get Steward by given guid.",
            operationId = "get_entity_attributes_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataEntityAttributes has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "DataEntityAttribute with entity_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{entity_id}/attributes", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"lo_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<DataEntityAttribute>> getAllEntityAttributesPaginated(
            @Parameter(description = "Artifact ID of the Entity Attribute",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("entity_id") String entityId,
            @Parameter(description = "The maximum number of Stewards to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "99999") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers
    ) throws Exception {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(entityService.getEntityAttributesWithPaging(entityId, offset, limit, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Create DataEntityAttribute.",
            description = "This method can be used to get Steward by given guid.",
            operationId = "create_entity_attribute"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataEntityAttribute has been added successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{entity_id}/attributes", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"lo_r", "lo_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<DataEntityAttribute> createEntityAttribute(
            @Parameter(description = "Artifact ID of the Entity",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("entity_id") String entityId,
            @RequestBody UpdatableDataEntityAttributeEntity newDataEntityAttributeEntity,
            @RequestHeader HttpHeaders headers) throws Exception {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(entityService.createEntityAttribute(entityId, newDataEntityAttributeEntity, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Update DataEntityAttribute.",
            description = "This method can be used to updating DataEntityAttribute by given guid.",
            operationId = "update_entity_attribute"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataEntityAttribute has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "DataEntityAttribute with entity_attribute_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/attributes/{entity_attribute_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"lo_r", "lo_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<DataEntityAttribute> patchEntityAttribute(
            @Parameter(description = "Artifact ID of the Entity Attribute",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("entity_attribute_id") String entityAttributeId,
            @RequestBody UpdatableDataEntityAttributeEntity dataEntityAttributeEntity,
            @RequestHeader HttpHeaders headers) throws Exception {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(entityService.patchEntityAttribute(entityAttributeId, dataEntityAttributeEntity, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Delete DataEntityAttribute.",
            description = "This method can be used to deleting DataEntityAttribute by given guid.",
            operationId = "delete_entity_attribute"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataEntityAttribute has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "DataEntityAttribute with entity_attribute_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/attributes/{entity_attribute_id}/{force}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"lo_r", "lo_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<ArchiveResponse> deleteEntityAttribute(
            @Parameter(description = "Artifact ID of the Entity Attribute",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("entity_attribute_id") String entityAttributeId,
            @Parameter(description = "Filter for cascade removal",
                    example = "true")
            @PathVariable("force") Boolean force,
            @RequestHeader HttpHeaders headers) throws Exception {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(entityService.deleteEntityAttribute(entityAttributeId, force, userDetails), HttpStatus.OK);
    }

    // Folders

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets DataEntityFolder by given guid.",
            description = "This method can be used to get DataEntityFolder by given guid.",
            operationId = "get_entity_folder_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataEntityFolder has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "DataEntityFolder with folder_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/folders/{folder_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"lo_r"}, level = ANY_ROLE)
    public ResponseEntity<DataEntityFolder> getFolderById(
            @Parameter(description = "Artifact ID of the Entity Folder",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("folder_id") String folderId,
            @RequestParam(value="include_children", defaultValue = "true") Boolean includeChildren,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(entityService.getDataEntityFolderById(folderId, includeChildren, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all DataEntityFolder by given guid.",
            description = "This method can be used to get all DataEntityFolder by given guid.",
            operationId = "get_root_folders"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All DataEntityFolder has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "DataEntityFolder not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/folders/root", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"lo_r"}, level = ANY_ROLE)
    public ResponseEntity<List<DataEntityFolder>> getRootFolders(
            @RequestParam(value="include_children", defaultValue = "true") Boolean includeChildren,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(entityService.getRootFolders(includeChildren, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Create DataEntityFolder.",
            description = "This method can be used to creating DataEntityFolder.",
            operationId = "create_entity_folder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataEntityFolder has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/folder", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"lo_r", "lo_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<DataEntityFolder> createFolder(
            @RequestBody UpdatableDataEntityFolderEntity newDataEntityFolderEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(entityService.createFolder(newDataEntityFolderEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Update DataEntityFolder.",
            description = "This method can be used to updating DataEntityFolder by given guid.",
            operationId = "update_entity_folder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataEntityFolder has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "DataEntityFolder with folder_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/folders/{folder_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"lo_r", "lo_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<DataEntityFolder> patchFolder(
            @Parameter(description = "Artifact ID of the Entity Folder",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("folder_id") String folderId,
            @RequestBody UpdatableDataEntityFolderEntity dataEntityFolderEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(entityService.patchFolder(folderId, dataEntityFolderEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Delete DataEntityFolder.",
            description = "This method can be used to deleting DataEntityFolder by given guid.",
            operationId = "delete_entity_folder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataEntityFolder has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "DataEntityFolder with folder_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/folders/{folder_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"lo_r", "lo_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<ArchiveResponse> deleteFolder(
            @Parameter(description = "Artifact ID of the Entity Folder",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("folder_id") String folderId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(entityService.deleteFolder(folderId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"lo_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatDataEntity>> searchDataEntities(
            @RequestBody SearchRequestWithJoin request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        SearchResponse<FlatDataEntity> res = entityService.searchDataEntities(request, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/search_by_domain/{domain_id}", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"lo_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatDataEntity>> searchDataEntityByDomain(
            @RequestBody SearchRequestWithJoin request,
            @PathVariable("domain_id") String domainId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        SearchResponse<FlatDataEntity> res = entityService.searchDataEntityByDomain(request, domainId, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/search_by_indicator/{indicator_id}", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"lo_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatDataEntity>> searchDataEntityByIndicator(
            @RequestBody SearchRequestWithJoin request,
            @PathVariable("indicator_id") String indicatorId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        SearchResponse<FlatDataEntity> res = entityService.searchDataEntityByIndicator(request, indicatorId, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/search_by_be/{be_id}", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"lo_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatDataEntity>> searchDataEntityByBE(
            @RequestBody SearchRequestWithJoin request,
            @PathVariable("be_id") String beId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        SearchResponse<FlatDataEntity> res = entityService.searchDataEntityByBE(request, beId, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/search_attributes_by_entity_id/{entity_id}", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"lo_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatDataEntityAttribute>> searchAttributesByEntityId(
            @RequestBody SearchRequestWithJoin request,
            @PathVariable("entity_id") String entityId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        SearchResponse<FlatDataEntityAttribute> res = entityService.searchAttributesByEntityId(request, entityId, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/search_attributes", method = RequestMethod.POST, produces = { "application/json"})
    public ResponseEntity<SearchResponse<FlatDataEntityAttribute>> searchAttributes(
            @RequestBody SearchRequestWithJoin request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        SearchResponse<FlatDataEntityAttribute> res = entityService.searchAttributes(request, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/attributes/types", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"lo_r"}, level = ANY_ROLE)
    public ResponseEntity<List<EntityAttributeType>> getEntityAttributeTypes(@RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        List<EntityAttributeType> res = entityService.getEntityAttributeTypes(userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/model", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = {"lo_mdl_r"}, level = ANY_ROLE)
    public ResponseEntity<GojsModelData> getModel(@RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        GojsModelData res = entityService.getModel(userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/model", method = RequestMethod.PATCH, produces = { "application/json" })
    @Secured(roles = {"lo_mdl_u"}, level = ANY_ROLE)
    @CrossOrigin
    public ResponseEntity<List<GojsModelNodeData>> updateModel(@RequestBody UpdatableGojsModelData updatableGojsModelData, @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        List<GojsModelNodeData> res = entityService.updateModel(updatableGojsModelData, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }
}
