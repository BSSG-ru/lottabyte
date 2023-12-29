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
import ru.bssg.lottabyte.core.model.businessEntity.*;
import ru.bssg.lottabyte.core.model.datatype.DataType;
import ru.bssg.lottabyte.core.model.datatype.FlatDataType;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.BusinessEntityService;

import java.util.Collections;
import java.util.List;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "Business Entity", description = "APIs for Business Entity.")
@RestController
@Slf4j
@RequestMapping("v1/business_entities")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class BusinessEntityController {

    private final BusinessEntityService businessEntityService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Business Entity by given guid.",
            description = "This method can be used to get Indicator by given guid.",
            operationId = "getBusinessEntityById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Business Entity has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Business Entity with specified id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{business_entity_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"business_entity_r"}, level = ANY_ROLE)
    public ResponseEntity<BusinessEntity> getBusinessEntityById(
            @Parameter(description = "Artifact ID of the Business Entity",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("business_entity_id") String businessEntityId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(businessEntityService.getBusinessEntityById(businessEntityId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }



    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all Business Entities paginated.",
            description = "This method can be used to get all Indicators with paging.",
            operationId = "getBusinessEntitiesPaginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Business Entities have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"business_entity_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<BusinessEntity>> getBusinessEntitiesPaginated(
            @Parameter(description = "The maximum number of Business Entities to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @Parameter(description = "Artifact state.")
            @RequestParam(value="state", defaultValue = "PUBLISHED") String artifactState,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        return new ResponseEntity<>(businessEntityService.getBusinessEntitiesPaginated(offset, limit,
                artifactState, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates new Business Entity.",
            description = "This method can be used to create Indicator.",
            operationId = "createBusinessEntity"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Business Entity has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"business_entity_r", "business_entity_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<BusinessEntity> createBusinessEntity(
            @RequestBody UpdatableBusinessEntityEntity newBusinessEntityEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(businessEntityService.createBusinessEntity(newBusinessEntityEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Updates Business Entity by given guid.",
            description = "This method can be used to update DataAsset.",
            operationId = "patchBusinessEntity"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Indicator has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Indicator with specified id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{business_entity_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"business_entity_r", "business_entity_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<BusinessEntity> patchBusinessEntity(
            @Parameter(description = "Artifact ID of the Business Entity to be patched",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("business_entity_id") String businessEntityId,
            @RequestBody UpdatableBusinessEntityEntity businessEntityEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(businessEntityService.patchBusinessEntity(businessEntityId, businessEntityEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Deletes Business Entity by given guid.",
            description = "This method can be used to delete Indicator by id.",
            operationId = "deleteBusinessEntity"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Indicator has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Indicator with specified id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{business_entity_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"business_entity_r", "business_entity_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<?> deleteBusinessEntity(
            @Parameter(description = "Artifact ID of the Business Entity to be deleted",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("business_entity_id") String businessEntityId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        BusinessEntity result = businessEntityService.deleteBusinessEntity(businessEntityId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        if (result == null) {
            ArchiveResponse resp = new ArchiveResponse();
            resp.setDeletedGuids(Collections.singletonList(businessEntityId));
            return ResponseEntity.ok(resp);
        } else {
            return ResponseEntity.ok(result);
        }
    }

    @Hidden
    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"business_entity_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatBusinessEntity>> searchBusinessEntity(
            @RequestBody SearchRequestWithJoin request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(businessEntityService.searchBusinessEntity(request, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/tree", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"business_entity_r"}, level = ANY_ROLE)
    public ResponseEntity<List<BusinessEntityTreeNode>> getBETree(
            @RequestBody SearchRequestWithJoin request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(businessEntityService.getBETree(request, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Business Entity versions list by domain guid.",
            description = "This method can be used to get Business Entity history versions by given guid.",
            operationId = "getBusinessEntityVersionsById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Business Entity versions have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{business_entity_id}/versions", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"business_entity_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<BusinessEntity>> getBusinessEntityVersionsById(
            @PathVariable("business_entity_id") String businessEntityId,
            @Parameter(description = "The maximum number of Business Entity versions to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(businessEntityService.getBusinessEntityVersions(businessEntityId,
                offset, limit, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Business Entity version by given guid and version id.",
            description = "This method can be used to get Business Entity history version by given guid and version id.",
            operationId = "getBusinessEntityVersionById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data Asset version has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Data Asset version not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{business_entity_id}/versions/{version_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"business_entity_r"}, level = ANY_ROLE)
    public ResponseEntity<BusinessEntity> getBusinessEntityVersionById(
            @Parameter(description = "ID of the Business Entity", example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("business_entity_id") String businessEntityId,
            @Parameter(description = "Version ID of the Business Entity", example = "1")
            @PathVariable("version_id") Integer versionId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return ResponseEntity.ok(businessEntityService.getBusinessEntityVersionById(businessEntityId, versionId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
    }

}
