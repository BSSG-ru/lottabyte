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
import ru.bssg.lottabyte.core.model.dataentity.*;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRule;
import ru.bssg.lottabyte.core.model.entitySample.FlatEntitySampleDQRule;
import ru.bssg.lottabyte.core.model.entitySample.UpdatableEntitySampleDQRule;
import ru.bssg.lottabyte.core.model.indicator.FlatIndicator;
import ru.bssg.lottabyte.core.model.indicator.Indicator;
import ru.bssg.lottabyte.core.model.indicator.UpdatableIndicatorEntity;
import ru.bssg.lottabyte.core.ui.model.IndicatorType;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.ui.model.SystemType;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.EntitySampleService;
import ru.bssg.lottabyte.coreapi.service.IndicatorService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "Indicator", description = "APIs for Indicator.")
@RestController
@Slf4j
@RequestMapping("v1/indicators")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
@RequiredArgsConstructor
public class IndicatorController {

        private final IndicatorService indicatorService;
        private final EntitySampleService sampleService;
        private final JwtHelper jwtHelper;

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Indicator by given guid.", description = "This method can be used to get Indicator by given guid.", operationId = "getIndicatorById")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Indicator has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Indicator with specified id not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{indicator_id}", method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "indicator_r" }, level = ANY_ROLE)
        public ResponseEntity<Indicator> getIndicatorById(
                        @Parameter(description = "Artifact ID of the Indicator", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("indicator_id") String indicatorId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                return new ResponseEntity<>(indicatorService.getIndicatorById(indicatorId,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets EntityAttributes by given guid.", description = "This method can be used to get EntityAttributes by given guid.", operationId = "getEntityAttributesByIndicatorId")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "EntityAttributes has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "EntityAttributes with specified id not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/entity_attributes_by_indicator/{indicator_id}", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "indicator_r" }, level = ANY_ROLE)
        public ResponseEntity<Map<String, List<DataEntityAttributeEntity>>> getEntityAttributesByIndicatorId(
                        @Parameter(description = "Artifact ID of the Indicator", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("indicator_id") String indicatorId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                return new ResponseEntity<>(indicatorService.getEntityAttributesByIndicatorId(indicatorId,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets all Indicators paginated.", description = "This method can be used to get all Indicators with paging.", operationId = "getIndicatorsPaginated")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Indicators have been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "", method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "indicator_r" }, level = ANY_ROLE)
        public ResponseEntity<PaginatedArtifactList<Indicator>> getIndicatorsPaginated(
                        @Parameter(description = "The maximum number of Indicators to return - must be at least 1 and cannot exceed 200. The default value is 10.") @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                        @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.") @RequestParam(value = "offset", defaultValue = "0") Integer offset,
                        @Parameter(description = "Artifact state.")
                        @RequestParam(value="state", defaultValue = "PUBLISHED") String artifactState,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                return new ResponseEntity<>(indicatorService.getIndicatorsPaginated(offset, limit, artifactState,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Creates new Indicator.", description = "This method can be used to create Indicator.", operationId = "createIndicator")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Indicator has been created successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "indicator_r", "indicator_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<Indicator> createIndicator(
                        @RequestBody UpdatableIndicatorEntity newIndicatorEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                return new ResponseEntity<>(indicatorService.createIndicator(newIndicatorEntity,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Updates Indicator by given guid.", description = "This method can be used to update DataAsset.", operationId = "patchIndicator")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Indicator has been updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Indicator with specified id not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{indicator_id}", method = RequestMethod.PATCH, produces = { "application/json" })
        @Secured(roles = { "indicator_r", "indicator_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<Indicator> patchIndicator(
                        @Parameter(description = "Artifact ID of the Indicator to be patched", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("indicator_id") String indicatorId,
                        @RequestBody UpdatableIndicatorEntity indicatorEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                return new ResponseEntity<>(indicatorService.patchIndicator(indicatorId, indicatorEntity,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Deletes Indicator by given guid.", description = "This method can be used to delete Indicator by id.", operationId = "deleteIndicator")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Indicator has been deleted successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Indicator with specified id not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{indicator_id}", method = RequestMethod.DELETE, produces = { "application/json" })
        @Secured(roles = { "indicator_r", "indicator_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<?> deleteIndicator(
                        @Parameter(description = "Artifact ID of the Indicator to be deleted", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("indicator_id") String indicatorId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                Indicator result = indicatorService.deleteIndicator(indicatorId,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
                if (result == null) {
                        ArchiveResponse resp = new ArchiveResponse();
                        resp.setDeletedGuids(Collections.singletonList(indicatorId));
                        return ResponseEntity.ok(resp);
                } else {
                        return ResponseEntity.ok(result);
                }
        }

        @Hidden
        @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json" })
        public ResponseEntity<SearchResponse<FlatIndicator>> searchIndicators(
                        @RequestBody SearchRequestWithJoin request,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                return new ResponseEntity<>(indicatorService.searchIndicators(request,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Indicator versions list by domain guid.", description = "This method can be used to get Indicator history versions by given guid.", operationId = "getIndicatorVersionsById")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Indicator versions have been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{indicator_id}/versions", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "indicator_r" }, level = ANY_ROLE)
        public ResponseEntity<PaginatedArtifactList<Indicator>> getIndicatorVersionsById(
                        @PathVariable("indicator_id") String indicatorId,
                        @Parameter(description = "The maximum number of Indicator versions to return - must be at least 1 and cannot exceed 200. The default value is 10.") @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                        @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.") @RequestParam(value = "offset", defaultValue = "0") Integer offset,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                return new ResponseEntity<>(indicatorService.getIndicatorVersions(indicatorId,
                                offset, limit, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets Indicator version by given guid and version id.", description = "This method can be used to get Indicator history version by given guid and version id.", operationId = "getIndicatorVersionById")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Data Asset version has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "Data Asset version not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{indicator_id}/versions/{version_id}", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "indicator_r" }, level = ANY_ROLE)
        public ResponseEntity<Indicator> getIndicatorVersionById(
                        @Parameter(description = "ID of the Indicator", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("indicator_id") String indicatorId,
                        @Parameter(description = "Version ID of the Indicator", example = "1") @PathVariable("version_id") Integer versionId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                return ResponseEntity.ok(indicatorService.getIndicatorVersionVersionById(indicatorId, versionId,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
        }

        @Hidden
        @RequestMapping(value = "/indicator_types", method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "indicator_r" }, level = ANY_ROLE)
        public ResponseEntity<List<IndicatorType>> getIndicatorTypes(
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                return new ResponseEntity<>(indicatorService.getIndicatorTypes(
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Hidden
        @RequestMapping(value = "/indicator_types/{id}", method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "indicator_r" }, level = ANY_ROLE)
        public ResponseEntity<IndicatorType> getIndicatorTypeById(
                        @PathVariable("id") String id,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                return new ResponseEntity<>(indicatorService.getIndicatorTypeById(id,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Hidden
        @RequestMapping(value = "/search_by_domain/{domain_id}", method = RequestMethod.POST, produces = {
                        "application/json" })
        @Secured(roles = { "lo_r" }, level = ANY_ROLE)
        public ResponseEntity<SearchResponse<FlatIndicator>> searchIndicatorsByDomain(
                        @RequestBody SearchRequestWithJoin request,
                        @PathVariable("domain_id") String domainId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = HttpUtils.getToken(headers);
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                SearchResponse<FlatIndicator> res = indicatorService.searchIndicatorsByDomain(request, domainId,
                                userDetails);

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Search dq rules.", description = ".", operationId = "getDQRules")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{indicator_id}/dq_rules", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "indicator_r" }, level = ANY_ROLE)
        public ResponseEntity<List<EntitySampleDQRule>> getDQRules(
                        @PathVariable("indicator_id") String indicatorId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                List<EntitySampleDQRule> res = indicatorService.getDQRules(indicatorId,
                                userDetails);

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Create dq rule by given guid.", description = "This method can be used to create dq rule by given guid.", operationId = "createDQRule")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{indicator_id}/dq_rules", method = RequestMethod.POST, produces = {
                        "application/json" })
        @Secured(roles = { "indicator_r", "indicator_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<EntitySampleDQRule> createDQRule(
                        @PathVariable("indicator_id") String indicatorId,
                        @RequestBody UpdatableEntitySampleDQRule newEntitySampleDQRuleEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);
                return new ResponseEntity<>(indicatorService.createDQRule(indicatorId, newEntitySampleDQRuleEntity,
                                userDetails), HttpStatus.OK);
        }

}
