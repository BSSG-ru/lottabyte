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
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.dqRule.DQRule;
import ru.bssg.lottabyte.core.model.dqRule.FlatDQRule;
import ru.bssg.lottabyte.core.model.dqRule.UpdatableDQRuleEntity;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.DQRuleService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "DQ rule", description = "APIs for DQ rule.")
@RestController
@Slf4j
@RequestMapping("v1/dq_rule")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
@RequiredArgsConstructor
public class DQRuleController {
        private final DQRuleService dqRuleService;
        private final JwtHelper jwtHelper;

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets DQRule by given guid.", description = "This method can be used to get DQRule by given guid.", operationId = "getDQRuleById")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "DQRule has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "DQRule not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{dq_rule_id}", method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "dq_rule_r" }, level = ANY_ROLE)
        public ResponseEntity<DQRule> getDQRuleById(
                        @Parameter(description = "ID of the DQRule", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("dq_rule_id") String dqRuleId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                DQRule d = dqRuleService.getDQRuleById(dqRuleId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
                return new ResponseEntity<>(d, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets DQRule version by given guid and version id.", description = "This method can be used to get DQRule history version by given guid and version id.", operationId = "getDQRuleVersionById")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "DQRule has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "DQRule version not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{dq_rule_id}/versions/{version_id}", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "dq_rule_r" }, level = ANY_ROLE)
        public ResponseEntity<DQRule> getDQRuleVersionById(
                        @Parameter(description = "ID of the DQRule", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("dq_rule_id") String dqRuleId,
                        @Parameter(description = "Version ID of the DQRule", example = "1") @PathVariable("version_id") Integer versionId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                DQRule d = dqRuleService.getDQRuleVersionById(dqRuleId, versionId,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
                return new ResponseEntity<>(d, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets DQRule versions list by dqRule guid.", description = "This method can be used to get DQRule history versions by given guid.", operationId = "getDQRuleVersionsById")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "DQRule versions have been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{dq_rule_id}/versions", method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "dq_rule_r" }, level = ANY_ROLE)
        public ResponseEntity<PaginatedArtifactList<DQRule>> getDQRuleVersionsById(
                        @PathVariable("dq_rule_id") String dqRuleId,
                        @Parameter(description = "The maximum number of DQRule versions to return - must be at least 1 and cannot exceed 200. The default value is 10.") @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                        @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.") @RequestParam(value = "offset", defaultValue = "0") Integer offset,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                PaginatedArtifactList<DQRule> list = dqRuleService.getDQRuleVersions(dqRuleId, offset, limit,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
                return new ResponseEntity<>(list, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Gets paginated DQRules.", description = "This method can be used to get all DQRules with paging.", operationId = "getDQRulesPaginated")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "DQRules have been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "404", description = "DQRules not found"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "", method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "dq_rule_r" }, level = ANY_ROLE)
        public ResponseEntity<PaginatedArtifactList<DQRule>> getDQRulesPaginated(
                        @Parameter(description = "The maximum number of DQRules to return - must be at least 1 and cannot exceed 200. The default value is 10.") @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                        @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.") @RequestParam(value = "offset", defaultValue = "0") Integer offset,
                        @Parameter(description = "Artifact state.") @RequestParam(value = "state", defaultValue = "PUBLISHED") String artifactState,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                PaginatedArtifactList<DQRule> list = dqRuleService.getDQRulesPaginated(offset, limit, artifactState,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
                return new ResponseEntity<>(list, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Creates DQRule.", description = "This method can be used to create DQRule.", operationId = "createDQRule")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "DQRule has been created successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "", method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "dq_rule_r", "dq_rule_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<DQRule> createDQRule(@RequestBody UpdatableDQRuleEntity newDQRuleEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                DQRule d = dqRuleService.createDQRule(newDQRuleEntity,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
                return new ResponseEntity<>(d, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Updates DQRule by given guid.", description = "This method can be used to update DQRule by given guid.", operationId = "patchDQRule")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "DQRule has been updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{dq_rule_id}", method = RequestMethod.PATCH, produces = { "application/json" })
        @Secured(roles = { "dq_rule_r", "dq_rule_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<DQRule> patchDQRule(
                        @Parameter(description = "ID of the DQRule", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("dq_rule_id") String dqRuleId,
                        @RequestBody UpdatableDQRuleEntity dqRuleEntity,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                DQRule d = dqRuleService.updateDQRule(dqRuleId, dqRuleEntity,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
                return new ResponseEntity<>(d, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Deletes DQRule by given guid.", description = "This method can be used to delete DQRule by given guid.", operationId = "get_custom_attribute_definition_by_id")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "DQRule has been deleted successfully for DRAFT dqRule. DQRule draft marked for removal has been created for PUBLISHED dqRule"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{dq_rule_id}", method = RequestMethod.DELETE, produces = { "application/json" })
        @Secured(roles = { "dq_rule_r", "dq_rule_u" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<?> deleteDQRule(
                        @Parameter(description = "ID of the DQRule", example = "aa0e33f5-3108-4d45-a530-0307458362d4") @PathVariable("dq_rule_id") String dqRuleId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                DQRule result = dqRuleService.deleteDQRuleById(dqRuleId,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
                if (result == null) {
                        ArchiveResponse resp = new ArchiveResponse();
                        resp.setDeletedGuids(Collections.singletonList(dqRuleId));
                        return ResponseEntity.ok(resp);
                } else {
                        return ResponseEntity.ok(result);
                }
        }

        @Hidden
        @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "dq_rule_r" }, level = ANY_ROLE)
        public ResponseEntity<SearchResponse<FlatDQRule>> searchDQRules(
                        @RequestBody SearchRequestWithJoin request,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {

                return ResponseEntity.ok(dqRuleService.searchDQRules(request,
                                jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
        }

        @Hidden
        @RequestMapping(value = "/rule_types", method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "dq_rule_r" }, level = ANY_ROLE)
        public ResponseEntity<List<DQRuleType>> getRuleTypes(
                @RequestHeader HttpHeaders headers) throws LottabyteException {
                return new ResponseEntity<>(dqRuleService.getRuleTypes(
                        jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }

        @Hidden
        @RequestMapping(value = "/rule_types/{id}", method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "dq_rule_r" }, level = ANY_ROLE)
        public ResponseEntity<DQRuleType> getRuleTypeById(
                @PathVariable("id") String id,
                @RequestHeader HttpHeaders headers) throws LottabyteException {
                return new ResponseEntity<>(dqRuleService.getRuleTypeById(id,
                        jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
        }
}
