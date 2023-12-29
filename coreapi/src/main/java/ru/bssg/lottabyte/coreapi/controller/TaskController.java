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
import ru.bssg.lottabyte.core.model.task.Task;
import ru.bssg.lottabyte.core.model.task.UpdatableTaskEntity;
import ru.bssg.lottabyte.core.model.taskrun.TaskRun;
import ru.bssg.lottabyte.core.model.task.FlatTask;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.EntitySampleService;
import ru.bssg.lottabyte.coreapi.service.TaskRunService;
import ru.bssg.lottabyte.coreapi.service.TaskService;

import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "Task", description = "APIs for Tasks.")
@RestController
@Slf4j
@RequestMapping("v1/tasks")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    private final EntitySampleService entitySampleService;
    private final JwtHelper jwtHelper;
    private final TaskRunService taskRunService;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Get Task.",
            description = "Get Task.",
            operationId = "task_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get Task"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{task_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"task_r"}, level = ANY_ROLE)
    public ResponseEntity<Task> getTaskById(
            @PathVariable("task_id") String taskId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(taskService.getTaskById(taskId, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Get Task with PaginatedArtifactList.",
            description = "Get Task."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get Task with PaginatedArtifactList"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"task_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<Task>> getTasksPaginated(
            @Parameter(description = "The maximum number of Tasks to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(taskService.getTasksWithPaging(limit, offset, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Get Tasks by query id with PaginatedArtifactList.",
            description = "Get Tasks by query id."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get Task with PaginatedArtifactList"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/by_query/{query_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"task_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<Task>> getTasksByQueryId(
            @Parameter(description = "Query Id")
            @PathVariable(value="query_id") String queryId,
            @Parameter(description = "The maximum number of Tasks to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        UserDetails userDetails = jwtHelper.getUserDetail(HttpUtils.getToken(headers));
        return new ResponseEntity<>(taskService.getTasksByQueryId(queryId, limit, offset, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Create Task.",
            description = "Create Task.",
            operationId = "newTaskEntity"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Create Task"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"task_r", "task_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Task> createTask(
            @RequestBody UpdatableTaskEntity newTaskEntity,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(taskService.createTask(newTaskEntity, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Update Task.",
            description = "Update Task.",
            operationId = "newTaskEntity"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Update Task"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{task_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"task_r", "task_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Task> updateTask(
            @Parameter(description = "Artifact ID of the Task",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("task_id") String taskId,
            @RequestBody UpdatableTaskEntity taskEntity,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(taskService.updateTask(taskId, taskEntity, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Delete Task.",
            description = "Delete Task.",
            operationId = "taskId"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Delete Task"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{task_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"task_r", "task_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<ArchiveResponse> deleteTask(
            @Parameter(description = "Artifact ID of the Task",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("task_id") String taskId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(taskService.deleteTask(taskId, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Get Sample Properties from custom jdbc for test.",
            description = "Get Sample Properties from custom jdbc for test.",
            operationId = "task_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sample properties have been obtained"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/run/test/{task_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"task_r", "task_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<String> getSamplesPropertiesForTest(
            @PathVariable("task_id") String taskId,
            @Parameter(description = "The number of lines to be output. The default value is 50.")
            @RequestParam(value="rows_number", defaultValue = "50") Integer rowsNumber,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(entitySampleService.getSamplesPropertiesForTest(taskId, rowsNumber, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Get Sample Properties from custom jdbc.",
            description = "Get Sample Properties from custom jdbc.",
            operationId = "task_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sample properties have been obtained"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/run/{task_id}", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"task_r", "task_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<TaskRun> getSamplesProperties(
            @PathVariable("task_id") String taskId,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        TaskRun taskRun = entitySampleService.createTaskRunBeforeRequest(taskId, userDetails);

        entitySampleService.workWithConnectors(taskId, taskRun, userDetails);

        return new ResponseEntity<>(taskRun, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Get task run.",
            description = "Get task run.",
            operationId = "task_run_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task run have been obtained"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/task/run_state/{task_run_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"task_r"}, level = ANY_ROLE)
    public ResponseEntity<TaskRun> getTaskRunById(
            @PathVariable("task_run_id") String taskRunId,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(taskRunService.getTaskRunById(taskRunId, userDetails), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"task_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatTask>> searchTaskRuns(
            @RequestBody SearchRequestWithJoin request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        SearchResponse<FlatTask> res = taskService.searchTaskRuns(request, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }
}
