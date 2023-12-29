package ru.bssg.lottabyte.scheduler.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.scheduler.service.SchedulerService;

import java.util.List;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;


@Tag(name = "Task", description = "APIs for Tasks.")
@RestController
@Slf4j
@RequestMapping("/v1/admin")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class SchedulerController {
    private final SchedulerService schedulerService;
    private final JwtHelper jwtHelper;

    @Autowired
    public SchedulerController(SchedulerService schedulerService, JwtHelper jwtHelper) {
        this.schedulerService = schedulerService;
        this.jwtHelper = jwtHelper;
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Get Task.",
            description = "Get Task."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get Task"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"admin"}, level = ANY_ROLE)
    public ResponseEntity<?> getActiveTasks(
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(schedulerService.getActiveTasks(userDetails), HttpStatus.OK);
    }
}
