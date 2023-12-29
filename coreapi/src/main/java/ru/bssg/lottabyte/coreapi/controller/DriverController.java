package ru.bssg.lottabyte.coreapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.coreapi.service.DriverService;

import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@RestController
@Slf4j
@RequestMapping("v1/data_storage")
@RequiredArgsConstructor
public class DriverController {
    private final DriverService driverService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Uploads a driver to the storage.",
            description = "This method can be used to uploads a driver to the storage.",
            operationId = "upload_drivers"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Drivers uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Drivers didn't load"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/upload_drivers", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"admin"}, level = ANY_ROLE)
    public ResponseEntity<?> uploadDrivers(
            @RequestParam("drivers") MultipartFile[] drivers,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        driverService.uploadDrivers(drivers, userDetails);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
