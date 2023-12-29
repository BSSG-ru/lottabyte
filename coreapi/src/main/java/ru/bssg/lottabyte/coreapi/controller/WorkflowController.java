package ru.bssg.lottabyte.coreapi.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.FlatWorkflowProcessDefinition;
import ru.bssg.lottabyte.core.model.UpdatableWorkflowProcessDefinition;
import ru.bssg.lottabyte.core.model.WorkflowProcessDefinition;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.domain.FlatDomain;
import ru.bssg.lottabyte.core.model.domain.UpdatableDomainEntity;
import ru.bssg.lottabyte.core.model.workflow.WorkflowActionParamResult;
import ru.bssg.lottabyte.core.model.workflow.WorkflowActionResultWrapper;
import ru.bssg.lottabyte.core.model.workflow.WorkflowTask;
import ru.bssg.lottabyte.core.ui.model.SearchRequest;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.WorkflowService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;
import java.net.URLDecoder;

@Tag(name = "Workflow", description = "APIs for Workflow.")
@RestController
@Slf4j
@CrossOrigin
@RequestMapping("v1/workflows")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class WorkflowController {

    private final JwtHelper jwtHelper;
    private final WorkflowService workflowService;
    private final RepositoryService repositoryService;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Workflow  Task by given guid.",
            description = "This method can be used to get Workflow task by given Workflow task guid.",
            operationId = "getWorkflowTaskById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Workflow task has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Workflow task not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/tasks/{workflow_task_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"workflow_r"}, level = ANY_ROLE)
    public ResponseEntity<WorkflowTask> getWorkflowTaskById(
            @Parameter(description = "ID of the Workflow task",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("workflow_task_id") String workflowTaskId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        WorkflowTask t = workflowService.getWorkflowTaskById(workflowTaskId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)), true);
        return ResponseEntity.ok(t);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Post Workflow Task action.",
            description = "This method can be used to post Workflow action to workflow task by given Workflow task guid.",
            operationId = "postWorkflowTaskAction"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Workflow task action has been processed successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Workflow task id or action not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/tasks/{workflow_task_id}/actions/{workflow_task_action}", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"workflow_r"}, level = ANY_ROLE)
    public ResponseEntity<WorkflowActionResultWrapper<?>> postWorkflowTaskAction(
            @Parameter(description = "ID of the Workflow task",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("workflow_task_id") String workflowTaskId,
            @Parameter(description = "Workflow task action",
                    example = "publish")
            @PathVariable("workflow_task_action") String workflowTaskAction,
            @RequestBody List<WorkflowActionParamResult> actionParams,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return ResponseEntity.ok(workflowService.postWorkflowTaskAction(workflowTaskId, workflowTaskAction, actionParams,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))));

    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Create draft of published version of workflow managed artifact.",
            description = "This method can be used to to create DRAFT version of artifact gived artifact type and guid.",
            operationId = "createDraft"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DRAFT artifact successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "PUBLISHED Artifact of type and guid not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/create/draft/{artifact_type}/{artifact_id}", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"workflow_r"}, level = ANY_ROLE)
    public ResponseEntity<?> createDraft(
            @Parameter(description = "Artifact type", example = "domain")
            @PathVariable("artifact_type") String artifactType,
            @Parameter(description = "ID of the artifact", example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("artifact_id") String artifactId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return ResponseEntity.ok(workflowService.createDraft(artifactType, artifactId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
    }

    @GetMapping(value = "/repository/list", produces = "application/json")
    public String listDeployments() {
        List<Deployment> deps = repositoryService.createDeploymentQuery().list();
        deps.stream().forEach(x -> log.info(x.toString()));
        List<ProcessDefinition> prdefs = repositoryService.createProcessDefinitionQuery().latestVersion().list();
        prdefs.stream().forEach(x -> log.info(x.toString()));
        return null;
    }

    @ApiOperation(value = "Create a new deployment",
            tags = {
            "Deployment" }, consumes = "multipart/form-data", produces = "application/json", notes = "The request body should contain data of type multipart/form-data. There should be exactly one file in the request, any additional files will be ignored. The deployment name is the name of the file-field passed in. If multiple resources need to be deployed in a single deployment, compress the resources in a zip and make sure the file-name ends with .bar or .zip.\n"
            + "\n"
            + "An additional parameter (form-field) can be passed in the request body with name tenantId. The value of this field will be used as the id of the tenant this deployment is done in.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Indicates the deployment was created."),
            @ApiResponse(responseCode = "400", description = "Indicates there was no content present in the request body or the content mime-type is not supported for deployment. The status-description contains additional information.")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "file", dataType = "file", paramType = "form", required = true)
    })
    @PostMapping(value = "/repository/deployments", produces = "application/json", consumes = "multipart/form-data")
    public DeploymentResponse uploadDeployment(@ApiParam(name = "deploymentKey") @RequestParam(value = "deploymentKey", required = false) String deploymentKey,
                                               @ApiParam(name = "deploymentName") @RequestParam(value = "deploymentName", required = false) String deploymentName,
                                               @ApiParam(name = "tenantId") @RequestParam(value = "tenantId", required = false) String tenantId,
                                               HttpServletRequest request, HttpServletResponse response) throws LottabyteException {

        if (!(request instanceof MultipartHttpServletRequest)) {
            throw new LottabyteException("Multipart request is required");
        }
        /*if (restApiInterceptor != null) {
            restApiInterceptor.executeNewDeploymentForTenantId(tenantId);
        }*/
        String queryString = request.getQueryString();
        Map<String, String> decodedQueryStrings = splitQueryString(queryString);

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

        if (multipartRequest.getFileMap().size() == 0) {
            throw new LottabyteException("Multipart request with file content is required");
        }

        MultipartFile file = multipartRequest.getFileMap().values().iterator().next();

        try {
            DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
            String fileName = file.getOriginalFilename();
            if (StringUtils.isEmpty(fileName) || !(fileName.endsWith(".bpmn20.xml") || fileName.endsWith(".bpmn") || fileName.toLowerCase().endsWith(".bar") || fileName.toLowerCase().endsWith(".zip"))) {
                fileName = file.getName();
            }

            if (fileName.endsWith(".bpmn20.xml") || fileName.endsWith(".bpmn")) {
                try (final InputStream fileInputStream = file.getInputStream()) {
                    deploymentBuilder.addInputStream(fileName, fileInputStream);
                }

            } else if (fileName.toLowerCase().endsWith(".bar") || fileName.toLowerCase().endsWith(".zip")) {
                try (InputStream fileInputStream = file.getInputStream();
                     ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {
                    deploymentBuilder.addZipInputStream(zipInputStream);
                }

            } else {
                throw new LottabyteException("File must be of type .bpmn20.xml, .bpmn, .bar or .zip");
            }

            if (!decodedQueryStrings.containsKey("deploymentName") || StringUtils.isEmpty(decodedQueryStrings.get("deploymentName"))) {
                String fileNameWithoutExtension = fileName.split("\\.")[0];

                if (StringUtils.isNotEmpty(fileNameWithoutExtension)) {
                    fileName = fileNameWithoutExtension;
                }

                deploymentBuilder.name(fileName);

            } else {
                deploymentBuilder.name(decodedQueryStrings.get("deploymentName"));
            }

            if (decodedQueryStrings.containsKey("deploymentKey") && StringUtils.isNotEmpty(decodedQueryStrings.get("deploymentKey"))) {
                deploymentBuilder.key(decodedQueryStrings.get("deploymentKey"));
            }

            if (tenantId != null) {
                deploymentBuilder.tenantId(tenantId);
            }

            /*if (restApiInterceptor != null) {
                restApiInterceptor.enhanceDeployment(deploymentBuilder);
            }*/

            Deployment deployment = deploymentBuilder.deploy();

            response.setStatus(HttpStatus.CREATED.value());
            return createDeploymentResponse(deployment);
        } catch (Exception e) {
            throw new LottabyteException(e.getMessage(), e);
        }
    }

    public Map<String, String> splitQueryString(String queryString) {
        if (StringUtils.isEmpty(queryString)) {
            return Collections.emptyMap();
        }
        Map<String, String> queryMap = new HashMap<>();
        for (String param : queryString.split("&")) {
            queryMap.put(StringUtils.substringBefore(param, "="), decode(StringUtils.substringAfter(param, "=")));
        }
        return queryMap;
    }

    protected String decode(String string) {
        if (string != null) {
            try {
                return URLDecoder.decode(string, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                throw new IllegalStateException("JVM does not support UTF-8 encoding.", uee);
            }
        }
        return null;
    }

    public DeploymentResponse createDeploymentResponse(Deployment deployment) {
        return new DeploymentResponse(deployment, "");
    }

    @Hidden
    @RequestMapping(value = "/searchSettings", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"workflow_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatWorkflowProcessDefinition>> searchSettings(
            @RequestBody SearchRequestWithJoin request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return ResponseEntity.ok(workflowService.searchSettings(request, jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Deletes WF settings by given guid.",
            description = "This method can be used to delete WF settings by given guid.",
            operationId = "delete_wf_Settings_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/settings/{id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"workflow_r", "workflow_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<?> deleteSettings(
            @Parameter(description = "ID",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("id") String id,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        WorkflowProcessDefinition result = workflowService.deleteSettingsById(id, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        if (result == null) {
            ArchiveResponse resp = new ArchiveResponse();
            resp.setDeletedGuids(Collections.singletonList(id));
            return ResponseEntity.ok(resp);
        } else {
            return ResponseEntity.ok(result);
        }
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets wf settings by given guid.",
            description = "This method can be used to get wf settings by given guid.",
            operationId = "getSettingsById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Domain not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/settings/{id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"workflow_r"}, level = ANY_ROLE)
    public ResponseEntity<WorkflowProcessDefinition> getSettingsById(
            @Parameter(description = "ID",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("id") String id,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        WorkflowProcessDefinition d = workflowService.getSettingsById(id, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(d, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Updates wf settings by given guid.",
            description = "This method can be used to update wf settings by given guid.",
            operationId = "patchSettings"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/settings/{id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"workflow_r", "workflow_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<WorkflowProcessDefinition> patchSettings(
            @Parameter(description = "ID",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("id") String id,
            @RequestBody UpdatableWorkflowProcessDefinition newPd,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        WorkflowProcessDefinition d = workflowService.updateSettings(id, newPd, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(d, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates wf settings.",
            description = "This method can be used to create wf settings.",
            operationId = "createSettings"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/settings", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"workflow_r", "workflow_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<WorkflowProcessDefinition> createSettings(@RequestBody UpdatableWorkflowProcessDefinition pd,
                                               @RequestHeader HttpHeaders headers) throws LottabyteException {
        WorkflowProcessDefinition d = workflowService.createSettings(pd, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(d, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Get process definitions.",
            description = "This method can be used to get process definitions.",
            operationId = "getProcessDefinitions"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/processDefinitions", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"workflow_r"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Map<String, String>> getFlowableProcessDefinitions(@RequestHeader HttpHeaders headers) throws LottabyteException {
        return new ResponseEntity<>(workflowService.getFlowableProcessDefinitions(jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

}
