package ru.bssg.lottabyte.coreapi.controller;

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
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.connector.Connector;
import ru.bssg.lottabyte.core.model.connector.ConnectorParam;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.coreapi.service.ConnectorService;

import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "Connector", description = "APIs for Connectors.")
@RestController
@Slf4j
@RequestMapping("v1/connectors")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class ConnectorController {
    private final ConnectorService connectorService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Connector by given guid.",
            description = "This method can be used to get Connector by given guid.",
            operationId = "get_connector_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Connector has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Connector with connector_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{connector_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"connector_r"}, level = ANY_ROLE)
    public ResponseEntity<Connector> getConnectorById(
            @Parameter(description = "ID of the Connector",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("connector_id") String connectorId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(connectorService.getConnectorById(connectorId, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all Connectors.",
            description = "This method can be used to get Connector by given guid.",
            operationId = "get_connectors_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Connector has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Connectors not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"connector_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<Connector>> getConnectorsPaginated(
            @Parameter(description = "The maximum number of Connectors to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(connectorService.getConnectorsPaginated(offset, limit), HttpStatus.OK);
}

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all ConnectorParam by given guid.",
            description = "This method can be used to get ConnectorParam by given guid.",
            operationId = "get_connector_param_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ConnectorParam has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "ConnectorParam with param_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/params/{param_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"connector_r"}, level = ANY_ROLE)
    public ResponseEntity<ConnectorParam> getConnectorParamById(
            @Parameter(description = "ID of the Connector Param",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("param_id") String paramId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(connectorService.getConnectorParamById(paramId), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all ConnectorParams by given guid.",
            description = "This method can be used to get ConnectorParam by given guid.",
            operationId = "get_connector_params_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ConnectorParam has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "ConnectorParam with connector_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{connector_id}/params", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"connector_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<ConnectorParam>> getConnectorParamsPaginated(
            @Parameter(description = "ID of the Connector",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("connector_id") String connectorId,
            @Parameter(description = "The maximum number of Connector Params to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(connectorService.getConnectorParamsPaginated(connectorId, offset, limit, userDetails), HttpStatus.OK);
    }

}
