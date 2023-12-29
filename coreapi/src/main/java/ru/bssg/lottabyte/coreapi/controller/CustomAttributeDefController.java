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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.ca.CustomAttributeDefElement;
import ru.bssg.lottabyte.core.model.ca.CustomAttributeDefinition;
import ru.bssg.lottabyte.core.model.ca.UpdatableCustomAttributeDefElementEntity;
import ru.bssg.lottabyte.core.model.ca.UpdatableCustomAttributeDefinitionEntity;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.CustomAttributeDefinitionService;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;

import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "CustomAttributeDef", description = "APIs for CustomAttributeDefinition.")
@RestController
@Slf4j
@RequestMapping("v1/custom_attribute")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class CustomAttributeDefController {
    private final CustomAttributeDefinitionService customAttributeDefinitionService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets CustomAttributeDefinition by given guid.",
            description = "This method can be used to get CustomAttributeDefinition by given guid.",
            operationId = "get_custom_attribute_definition_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CustomAttributeDefinition has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/definition/{definition_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"custom_attribute_r"}, level = ANY_ROLE)
    public ResponseEntity<CustomAttributeDefinition> getCustomAttributeDefinitionById(
            @Parameter(description = "Artifact ID of the CustomAttributeDefinition",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("definition_id") String customAttributeDefinitionId,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        return new ResponseEntity<>(customAttributeDefinitionService.getCustomAttributeDefinitionById(customAttributeDefinitionId,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all CustomAttributeDefinitions.",
            description = "This method can be used to get all CustomAttributeDefinitions.",
            operationId = "get_custom_attribute_definition_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CustomAttributeDefinitions have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/definition", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"custom_attribute_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<CustomAttributeDefinition>> getAllCustomAttributeDefinitionPaginated(
            @Parameter(description = "The maximum number of CustomAttributeDefinitions to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        return new ResponseEntity<>(customAttributeDefinitionService.getAllCustomAttributeDefinitionPaginated(offset, limit, null,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CustomAttributeDefinitions have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/definition/type/{artifact_type}", method = RequestMethod.GET, produces = { "application/json"})
    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all CustomAttributeDefinitions by artifact type.",
            description = "This method can be used to get all CustomAttributeDefinitions and filter by artifact type.",
            operationId = "getAllCustomAttributeDefinitionByArtifactTypePaginated"
    )
    @Secured(roles = {"custom_attribute_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<CustomAttributeDefinition>> getAllCustomAttributeDefinitionByArtifactTypePaginated(
            @Parameter(description = "Artifact Type to filter definitions",
                    example = "data_asset")
            @PathVariable("artifact_type") String artifactType,
            @Parameter(description = "The maximum number of CustomAttributeDefinitions to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        return new ResponseEntity<>(customAttributeDefinitionService.getAllCustomAttributeDefinitionPaginated(offset, limit, artifactType,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates CustomAttributeDefinition.",
            description = "This method can be used to create a new CustomAttributeDefinition.",
            operationId = "create_custom_attribute_definition"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "CustomAttributeDefinition has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/definition", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"custom_attribute_r", "custom_attribute_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<CustomAttributeDefinition> createCustomAttributeDefinition(
            @RequestBody UpdatableCustomAttributeDefinitionEntity newCustomAttributeDefinitionEntity,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        return new ResponseEntity<>(customAttributeDefinitionService.createCustomAttributeDefinition(newCustomAttributeDefinitionEntity,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Updates CustomAttributeDefinition.",
            description = "This method can be used to update CustomAttributeDefinition.",
            operationId = "patch_custom_attribute_definition"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CustomAttributeDefinition has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/definition/{definition_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"custom_attribute_r", "custom_attribute_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<CustomAttributeDefinition> patchCustomAttributeDefinition(
            @Parameter(description = "Artifact ID of the CustomAttributeDefinition",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("definition_id") String customAttributeDefinitionId,
            @RequestBody UpdatableCustomAttributeDefinitionEntity customAttributeDefinitionEntity,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        return new ResponseEntity<>(customAttributeDefinitionService.patchCustomAttributeDefinition(customAttributeDefinitionId,
                customAttributeDefinitionEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Deletes Custom Attribute Definition.",
            description = "This method can be used to delete Custom Attribute Definition.",
            operationId = "deleteCustomAttributeDefinition"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The CustomAttributeDefinition has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/definition/{definition_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"custom_attribute_r", "custom_attribute_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<ArchiveResponse> deleteCustomAttributeDefinition(
            @Parameter(description = "Artifact ID of the Custom Attribute Definition",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("definition_id") String customAttributeDefinitionId,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        return new ResponseEntity<>(customAttributeDefinitionService
                .deleteCustomAttributeDefinition(customAttributeDefinitionId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets CustomAttributeDefElement by given guid.",
            description = "This method can be used to get CustomAttributeDefElement by given guid.",
            operationId = "get_custom_attribute_def_element_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CustomAttributeDefElement has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/defelement/{defelement_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"custom_attribute_r"}, level = ANY_ROLE)
    public ResponseEntity<CustomAttributeDefElement> getCustomAttributeDefElementById(
            @Parameter(description = "Artifact ID of the CustomAttributeDefElement",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("defelement_id") String customAttributeDefElementId,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        return new ResponseEntity<>(customAttributeDefinitionService.getCustomAttributeDefElementById(customAttributeDefElementId,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all CustomAttributeDefElements.",
            description = "This method can be used to get all CustomAttributeDefElements.",
            operationId = "get_custom_attribute_def_element_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CustomAttributeDefElements have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/definition/{definition_id}/defelement", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"custom_attribute_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<CustomAttributeDefElement>> getAllCustomAttributeDefElementByDefinitionPaginated(
            @Parameter(description = "Artifact ID of the CustomAttributeDefinition",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("definition_id") String definitionId,
            @Parameter(description = "The maximum number of CustomAttributeDefElements to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        return new ResponseEntity<>(customAttributeDefinitionService.getAllCustomAttributeDefElementPaginated(offset, limit, definitionId,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CustomAttributeDefElements have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/defelement", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"custom_attribute_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<CustomAttributeDefElement>> getAllCustomAttributeDefElementPaginated(
            @Parameter(description = "The maximum number of CustomAttributeDefElements to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        return new ResponseEntity<>(customAttributeDefinitionService.getAllCustomAttributeDefElementPaginated(offset, limit, null,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates new CustomAttributeDefElement.",
            description = "This method can be used to create CustomAttributeDefElement.",
            operationId = "create_custom_attribute_def_element"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "CustomAttributeDefElement has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/defelement", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"custom_attribute_r", "custom_attribute_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<CustomAttributeDefElement> createCustomAttributeDefElement(
            @RequestBody UpdatableCustomAttributeDefElementEntity newCustomAttributeDefElementEntity,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        return new ResponseEntity<>(customAttributeDefinitionService.createCustomAttributeDefElement(newCustomAttributeDefElementEntity,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Updates CustomAttributeDefElement by given guid.",
            description = "This method can be used to update CustomAttributeDefElement.",
            operationId = "patch_custom_attribute_def_element"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CustomAttributeDefElement has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/defelement/{defelement_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"custom_attribute_r", "custom_attribute_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<CustomAttributeDefElement> patchCustomAttributeDefElement(
            @Parameter(description = "Artifact ID of the CustomAttributeDefElement",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("defelement_id") String customAttributeDefElementId,
            @RequestBody UpdatableCustomAttributeDefElementEntity customAttributeDefElementEntity,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");

        UserDetails userDetails = jwtHelper.getUserDetail(token);
        return new ResponseEntity<>(customAttributeDefinitionService.patchCustomAttributeDefElement (customAttributeDefElementId, customAttributeDefElementEntity, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Deletes CustomAttributeDefElement by given guid.",
            description = "This method can be used to delete CustomAttributeDefElement.",
            operationId = "delete_custom_attribute_def_element"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CustomAttributeDefElement has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/defelement/{defelement_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"custom_attribute_r", "custom_attribute_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<ArchiveResponse> deleteCustomAttributeDefElement(
            @Parameter(description = "Artifact ID of the CustomAttributeDefElement",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("defelement_id") String customAttributeDefElementId,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        String tenantId = jwtHelper.getTenant(token);
        return new ResponseEntity<>(customAttributeDefinitionService.deleteCustomAttributeDefElement(customAttributeDefElementId,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

}
