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
import ru.bssg.lottabyte.core.model.dataasset.DataAsset;
import ru.bssg.lottabyte.core.model.dataasset.FlatDataAsset;
import ru.bssg.lottabyte.core.model.dataasset.UpdatableDataAssetEntity;
import ru.bssg.lottabyte.core.model.dataentity.FlatDataEntity;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.DataAssetService;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "Data Assets", description = "APIs for Data Assets")
@RestController
@Slf4j
@RequestMapping("v1/data_assets")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class DataAssetController {
    private final DataAssetService dataAssetService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets DataAsset by given guid.",
            description = "This method can be used to get DataAsset by given guid.",
            operationId = "get_data_asset_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataAsset has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "DataAsset with data_asset_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{data_asset_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"active_r"}, level = ANY_ROLE)
    public ResponseEntity<DataAsset> getDataAssetById(
            @Parameter(description = "Artifact ID of the Data Asset",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("data_asset_id") String dataAssetId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(dataAssetService.getDataAssetById(dataAssetId, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all DataAsset.",
            description = "This method can be used to get DataAsset by given guid.",
            operationId = "get_data_assets_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataAssets has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "DataAssets not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"active_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<DataAsset>> getAllDataAssetPaginated(
            @Parameter(description = "The maximum number of Data Assets to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @Parameter(description = "Artifact state.")
            @RequestParam(value="state", defaultValue = "PUBLISHED") String artifactState,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(dataAssetService.getAllDataAssetPaginated(offset, limit, artifactState, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Get data asset by entity id.",
            description = "This method can be used to get data asset by entity id.",
            operationId = "get_data_assets_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataAssets has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "DataAssets not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/entity/{entity_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"active_r"}, level = ANY_ROLE)
    public ResponseEntity<List<DataAsset>> getDataAssetByEntityId(
            @Parameter(description = "id of the entity",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("entity_id") String entityId,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(dataAssetService.getDataAssetByEntityId(entityId, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates new DataAsset.",
            description = "This method can be used to create DataAsset.",
            operationId = "create_data_asset"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "DataAsset has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"active_r", "active_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<DataAsset> createDataAsset(
            @RequestBody UpdatableDataAssetEntity newDataAssetEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);
        return new ResponseEntity<>(dataAssetService.createDataAsset(newDataAssetEntity, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Updates DataAsset by given guid.",
            description = "This method can be used to update DataAsset.",
            operationId = "update_data_asset"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataAsset has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{data_asset_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"active_r", "active_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<DataAsset> patchDataAsset(
            @Parameter(description = "Artifact ID of the Data Asset",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("data_asset_id") String dataAssetId,
            @RequestBody UpdatableDataAssetEntity dataAssetEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);
        return new ResponseEntity<>(dataAssetService.patchDataAsset(dataAssetId, dataAssetEntity, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Deletes DataAsset by given guid.",
            description = "This method can be used to delete DataAsset.",
            operationId = "delete_data_asset"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DataAsset has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{data_asset_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"active_r", "active_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<?> deleteDataAsset(
            @Parameter(description = "Artifact ID of the Data Asset",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("data_asset_id") String dataAssetId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        DataAsset result = dataAssetService.deleteDataAsset(dataAssetId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        if (result == null) {
            ArchiveResponse resp = new ArchiveResponse();
            resp.setDeletedGuids(Collections.singletonList(dataAssetId));
            return ResponseEntity.ok(resp);
        } else {
            return ResponseEntity.ok(result);
        }
    }

    @Hidden
    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"active_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatDataAsset>> searchDataAssets(
            @RequestBody SearchRequestWithJoin request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails ud = jwtHelper.getUserDetail(token);

        SearchResponse<FlatDataAsset> res = dataAssetService.searchDataAssets(request, ud);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/search_by_be/{be_id}", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"lo_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatDataAsset>> searchDataAssetsByBE(
            @RequestBody SearchRequestWithJoin request,
            @PathVariable("be_id") String beId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        SearchResponse<FlatDataAsset> res = dataAssetService.searchDataAssetsByBE(request, beId, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Data Assets versions list by domain guid.",
            description = "This method can be used to get Data Asset history versions by given guid.",
            operationId = "getDataAssetVersionsById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data Asset versions have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{asset_id}/versions", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"active_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<DataAsset>> getDataAssetVersionsById(
            @PathVariable("asset_id") String assetId,
            @Parameter(description = "The maximum number of Data Asset versions to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        PaginatedArtifactList<DataAsset> list = dataAssetService.getDataAssetVersions(assetId,
                offset, limit, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Data Asset version by given guid and version id.",
            description = "This method can be used to get Data Asset history version by given guid and version id.",
            operationId = "getDataAssetVersionById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data Asset version has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Data Asset version not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{data_asset_id}/versions/{version_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"active_r"}, level = ANY_ROLE)
    public ResponseEntity<DataAsset> getDataAssetVersionById(
            @Parameter(description = "ID of the Data Asset", example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("data_asset_id") String dataAssetId,
            @Parameter(description = "Version ID of the Data Asset", example = "1")
            @PathVariable("version_id") Integer versionId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return ResponseEntity.ok(dataAssetService.getDataAssetVersionById(dataAssetId, versionId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
    }
}
