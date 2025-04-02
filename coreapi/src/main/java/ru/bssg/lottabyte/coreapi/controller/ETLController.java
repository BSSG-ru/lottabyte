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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.etl.ETL;
import ru.bssg.lottabyte.core.model.etl.FlatETL;
import ru.bssg.lottabyte.core.model.etl.UpdatableETLEntity;
import ru.bssg.lottabyte.core.model.indicator.FlatIndicator;
import ru.bssg.lottabyte.core.model.indicator.Indicator;
import ru.bssg.lottabyte.core.model.indicator.UpdatableIndicatorEntity;
import ru.bssg.lottabyte.core.ui.model.ETLType;
import ru.bssg.lottabyte.core.ui.model.IndicatorType;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.APILogService;
import ru.bssg.lottabyte.coreapi.service.ETLService;

import javax.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.List;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "ETL", description = "APIs for ETL.")
@RestController
@Slf4j
@RequestMapping("v1/etl")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
@RequiredArgsConstructor
public class ETLController {
    private final ETLService etlService;
    private final APILogService apiLogService;
    private final JwtHelper jwtHelper;

    @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets ETL by given guid.", description = "This method can be used to get ETL by given guid.", operationId = "getETLById")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ETL has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "ETL with specified id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{etl_id}", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = { "etl_r" }, level = ANY_ROLE)
    public ResponseEntity<ETL> getETLById(
            @Parameter(description = "Artifact ID of the ETL", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("etl_id") String etlId,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, etlId);
        return new ResponseEntity<>(etlService.getETLById(etlId,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Creates new ETL.", description = "This method can be used to create ETL.", operationId = "createETL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "ETL has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(method = RequestMethod.POST, produces = { "application/json" })
    @Secured(roles = { "etl_r", "etl_u" }, level = ALL_ROLES_STRICT)
    public ResponseEntity<ETL> createETL(
            @RequestBody UpdatableETLEntity newETLEntity,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, newETLEntity);
        return new ResponseEntity<>(etlService.createETL(newETLEntity,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Updates ETL by given guid.", description = "This method can be used to update ETL.", operationId = "patchETL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ETL has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "ETL with specified id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{etl_id}", method = RequestMethod.PATCH, produces = { "application/json" })
    @Secured(roles = { "etl_r", "etl_u" }, level = ALL_ROLES_STRICT)
    public ResponseEntity<ETL> patchETL(
            @Parameter(description = "Artifact ID of the ETL to be patched", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("etl_id") String etlId,
            @RequestBody UpdatableETLEntity etlEntity,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, etlId, etlEntity);
        return new ResponseEntity<>(etlService.patchETL(etlId, etlEntity, false,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Deletes ETL by given guid.", description = "This method can be used to delete ETL by id.", operationId = "deleteETL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ETL has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "ETL with specified id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{etl_id}", method = RequestMethod.DELETE, produces = { "application/json" })
    @Secured(roles = { "etl_r", "etl_u" }, level = ALL_ROLES_STRICT)
    public ResponseEntity<?> deleteETL(
            @Parameter(description = "Artifact ID of the ETL to be deleted", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("etl_id") String etlId,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, etlId);
        ETL result = etlService.deleteETL(etlId,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        if (result == null) {
            ArchiveResponse resp = new ArchiveResponse();
            resp.setDeletedGuids(Collections.singletonList(etlId));
            return ResponseEntity.ok(resp);
        } else {
            return ResponseEntity.ok(result);
        }
    }

    @Hidden
    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json" })
    public ResponseEntity<SearchResponse<FlatETL>> searchIndicators(
            @RequestBody SearchRequestWithJoin sr,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, sr);
        return new ResponseEntity<>(etlService.searchETL(sr,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets ETL versions list by domain guid.", description = "This method can be used to get ETL history versions by given guid.", operationId = "getETLVersionsById")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ETL versions have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{etl_id}/versions", method = RequestMethod.GET, produces = {
            "application/json" })
    @Secured(roles = { "etl_r" }, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<ETL>> getETLVersionsById(
            @PathVariable("etl_id") String etlId,
            @Parameter(description = "The maximum number of ETL versions to return - must be at least 1 and cannot exceed 200. The default value is 1.") @RequestParam(value = "limit", defaultValue = "1000") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.") @RequestParam(value = "offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, etlId, limit, offset);
        return new ResponseEntity<>(etlService.getETLVersions(etlId,
                offset, limit, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets ETL version by given guid and version id.", description = "This method can be used to get ETL history version by given guid and version id.", operationId = "getETLVersionById")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ETL version has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Data Asset version not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{etl_id}/versions/{version_id}", method = RequestMethod.GET, produces = {
            "application/json" })
    @Secured(roles = { "etl_r" }, level = ANY_ROLE)
    public ResponseEntity<ETL> getETLVersionById(
            @Parameter(description = "ID of the ETL", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("etl_id") String etlId,
            @Parameter(description = "Version ID of the ETL", example = "1") @PathVariable("version_id") Integer versionId,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, etlId, versionId);
        return ResponseEntity.ok(etlService.getETLVersionById(etlId, versionId,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Restores ETL version by given guid and version id.", description = "This method can be used to restore ETL history version by given guid and version id.", operationId = "restoreETLVersionById")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operation successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "ETL version not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{etl_id}/versions/{version_id}/restore", method = RequestMethod.POST, produces = {
            "application/json" })
    @Secured(roles = { "etl_u" }, level = ANY_ROLE)
    public ResponseEntity<ETL> restoreETLVersionById(
            @Parameter(description = "ID of the ETL", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("etl_id") String etlId,
            @Parameter(description = "Version ID of the ETL", example = "1") @PathVariable("version_id") Integer versionId,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, etlId, versionId);
        return ResponseEntity.ok(etlService.restoreETLVersionById(etlId, versionId,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
    }

    @Hidden
    @RequestMapping(value = "/etl_types", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = { "etl_r" }, level = ANY_ROLE)
    public ResponseEntity<List<ETLType>> getETLTypes(
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request);
        return new ResponseEntity<>(etlService.getETLTypes(
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/etl_types/{id}", method = RequestMethod.GET, produces = { "application/json" })
    @Secured(roles = { "etl_r" }, level = ANY_ROLE)
    public ResponseEntity<ETLType> getETLTypeById(
            @PathVariable("id") String id,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, id);
        return new ResponseEntity<>(etlService.getETLTypeById(id,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Archives ETL by given guid.",
            description = "This method can be used to archive ETL by given guid.",
            operationId = "archiveETL"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ETL has been archived successfully for DRAFT domain."),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/archive/{etl_id}", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"etl_r", "etl_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<?> archiveETL(
            @Parameter(description = "ID of the ETL",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("etl_id") String etlId,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, etlId);
        ETL result = etlService.archiveETLById(etlId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        if (result == null) {
            ArchiveResponse resp = new ArchiveResponse();
            resp.setDeletedGuids(Collections.singletonList(etlId));
            return ResponseEntity.ok(resp);
        } else {
            return ResponseEntity.ok(result);
        }
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Restores ETL by given guid.",
            description = "This method can be used to restore ETL by given guid.",
            operationId = "restoreETL"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ETL has been restored successfully for DRAFT domain."),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/restore/{etl_id}", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"etl_r", "etl_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<ETL> restoreETL(
            @Parameter(description = "ID of the ETL",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("etl_id") String etlId,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, etlId);
        ETL result = etlService.restoreETLById(etlId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
