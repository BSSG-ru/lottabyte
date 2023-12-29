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
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRule;
import ru.bssg.lottabyte.core.model.qualityTask.FlatQualityRuleTask;
import ru.bssg.lottabyte.core.model.qualityTask.FlatQualityTask;
import ru.bssg.lottabyte.core.model.qualityTask.QualityRuleTask;
import ru.bssg.lottabyte.core.model.qualityTask.QualityTask;
import ru.bssg.lottabyte.core.model.qualityTask.QualityTaskAssertion;
import ru.bssg.lottabyte.core.model.qualityTask.QualityTaskRun;
import ru.bssg.lottabyte.core.model.system.UpdatableSystemEntity;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;

import ru.bssg.lottabyte.coreapi.service.QualityTaskService;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "QualityTask", description = "APIs for Quality Task.")
@RestController
@Slf4j
@RequestMapping("v1/quality_tasks")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
@RequiredArgsConstructor
public class QualityTaskController {
        private final QualityTaskService qualityTaskService;
        private final JwtHelper jwtHelper;

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get Task .", description = "Get Task.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Get Task with PaginatedArtifactList"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(method = RequestMethod.GET, produces = { "application/json" })
        @Secured(roles = { "quality_task_r" }, level = ANY_ROLE)
        public ResponseEntity<PaginatedArtifactList<QualityTask>> getQualityTasksPaginated(
                        @Parameter(description = "The maximum number of Tasks to return - must be at least 1 and cannot exceed 200. The default value is 10.") @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                        @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.") @RequestParam(value = "offset", defaultValue = "0") Integer offset,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = HttpUtils.getToken(headers);
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                return new ResponseEntity<>(qualityTaskService.getTasksWithPaging(limit, offset, userDetails),
                                HttpStatus.OK);
        }

        @Hidden
        @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "quality_task_r" }, level = ANY_ROLE)
        public ResponseEntity<SearchResponse<FlatQualityTask>> searchQualityTask(
                        @RequestBody SearchRequestWithJoin request,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = HttpUtils.getToken(headers);
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                SearchResponse<FlatQualityTask> res = qualityTaskService.searchQualityTask(request, userDetails);

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get quality tasks.", description = ".", operationId = "getQualityTasksByRunId")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{run_id}", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "quality_task_r" }, level = ANY_ROLE)
        public ResponseEntity<List<QualityTask>> getQualityTasksByRunId(
                        @PathVariable("run_id") String runId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                List<QualityTask> res = qualityTaskService.getQualityTasksByRunId(runId,
                                userDetails);

                System.out.println("res len = " + res.size());

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get quality task assertions.", description = ".", operationId = "getQualityTasksAssertionByRunId")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/{run_id}/assertions", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "quality_task_r" }, level = ANY_ROLE)
        public ResponseEntity<List<QualityTaskAssertion>> getQualityTasksAssertionByRunId(
                        @PathVariable("run_id") String runId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                List<QualityTaskAssertion> res = qualityTaskService.getQualityTasksAssertionByRunId(runId,
                                userDetails);

                System.out.println("res len = " + res.size());

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get quality tasks.", description = ".", operationId = "getQualityRulesForSchedule")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/rules_for_schedule", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "quality_task_r" }, level = ANY_ROLE)
        public ResponseEntity<List<QualityRuleTask>> getQualityRulesForSchedule(
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                List<QualityRuleTask> res = qualityTaskService.getQualityRulesForSchedule(userDetails);

                System.out.println("res len = " + res.size());

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get quality tasks.", description = ".", operationId = "getQualityRulesForSchedule")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Sample body has been retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Bad request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Forbidden"),
                        @ApiResponse(responseCode = "500", description = "Internal Server error")
        })
        @RequestMapping(value = "/rules", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "quality_task_r" }, level = ANY_ROLE)
        public ResponseEntity<List<QualityRuleTask>> getQualityRules(
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                List<QualityRuleTask> res = qualityTaskService.getQualityRules(userDetails);

                System.out.println("res len = " + res.size());

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @RequestMapping(value = "/{ruleId}/rule_task", method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "quality_task_r" }, level = ALL_ROLES_STRICT)
        public ResponseEntity<?> createSystem(
                        @PathVariable("ruleId") String ruleId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                System.out.println("addQualityRuleTask = " + ruleId);
                qualityTaskService.addQualityRuleTask(ruleId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
                return ResponseEntity.ok(null);
        }

        @Hidden
        @RequestMapping(value = "/search_rules", method = RequestMethod.POST, produces = { "application/json" })
        @Secured(roles = { "quality_task_r" }, level = ANY_ROLE)
        public ResponseEntity<SearchResponse<FlatQualityRuleTask>> searchQualityRuleTasks(
                        @RequestBody SearchRequestWithJoin request,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = HttpUtils.getToken(headers);
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                SearchResponse<FlatQualityRuleTask> res = qualityTaskService.searchQualityRuleTask(request,
                                userDetails);

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

        @Operation(security = @SecurityRequirement(name = "bearerAuth"), summary = "Get quality task runs.", description = ".", operationId = "getQualityRuleRuns")

        @RequestMapping(value = "/{rule_id}/runs", method = RequestMethod.GET, produces = {
                        "application/json" })
        @Secured(roles = { "quality_task_r" }, level = ANY_ROLE)
        public ResponseEntity<List<QualityTaskRun>> getQualityRuleRuns(
                        @PathVariable("rule_id") String ruleId,
                        @RequestHeader HttpHeaders headers) throws LottabyteException {
                String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ",
                                "");
                UserDetails userDetails = jwtHelper.getUserDetail(token);

                List<QualityTaskRun> res = qualityTaskService.getQualityRuleRuns(ruleId,
                                userDetails);

                System.out.println("res len = " + res.size());

                return new ResponseEntity<>(res, HttpStatus.OK);
        }

}
