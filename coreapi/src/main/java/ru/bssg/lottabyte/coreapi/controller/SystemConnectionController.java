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
import ru.bssg.lottabyte.core.model.dataasset.UpdatableDataAssetEntity;
import ru.bssg.lottabyte.core.model.dataentity.DataEntity;
import ru.bssg.lottabyte.core.model.dataentity.UpdatableDataEntityEntity;
import ru.bssg.lottabyte.core.model.system.FlatSystemConnection;
import ru.bssg.lottabyte.core.model.system.SystemConnection;
import ru.bssg.lottabyte.core.model.system.SystemConnectionParam;
import ru.bssg.lottabyte.core.model.system.UpdatableSystemConnectionEntity;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.SystemConnectionService;

import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "System Connection", description = "APIs for System Connections.")
@RestController
@Slf4j
@RequestMapping("v1/system_connections")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class SystemConnectionController {
    private final SystemConnectionService systemConnectionService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates SystemConnection.",
            description = "This method can be used to creating SystemConnection.",
            operationId = "create_system_connection"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SystemConnection has been added successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"connection_r", "connection_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<SystemConnection> createSystemConnection(@RequestBody UpdatableSystemConnectionEntity updatableSystemConnectionEntity,
                                                   @RequestHeader HttpHeaders headers) throws LottabyteException
    {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(systemConnectionService.createSystemConnection(updatableSystemConnectionEntity, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Patch SystemConnection.",
            description = "This method can be used to updating SystemConnection.",
            operationId = "patch_system_connection"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SystemConnection has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{system_connection_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"connection_r", "connection_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<SystemConnection> patchSystemConnection(
            @Parameter(description = "Artifact ID of the Data Asset",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("system_connection_id") String systemConnectionId,
            @RequestBody UpdatableSystemConnectionEntity updatableSystemConnectionEntity,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(systemConnectionService.patchSystemConnection(systemConnectionId, updatableSystemConnectionEntity, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets System by given guid.",
            description = "This method can be used to get System by given guid.",
            operationId = "get_system_connection_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System connection has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "System not found with given id"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{system_connection_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"connection_r"}, level = ANY_ROLE)
    public ResponseEntity<SystemConnection> getSystemConnectionById(
            @Parameter(description = "ID of the Connector",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("system_connection_id") String connectorId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(systemConnectionService.getSystemConnectionById(connectorId, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Deletes System Connection by given guid.",
            description = "This method can be used to delete System Connection.",
            operationId = "deleteSystemConnection"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System Connection has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "System Connection not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{system_connection_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"connection_r", "connection_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<ArchiveResponse> deleteSystemConnection(
            @Parameter(description = "Artifact ID of the System Connection",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("system_connection_id") String systemConnectionId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return new ResponseEntity<>(systemConnectionService.deleteSystemConnection(systemConnectionId,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets System by given guid.",
            description = "This method can be used to get System by given guid.",
            operationId = "get_system_connections_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System connections have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "System not found with given id"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/system/{system_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"connection_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<SystemConnection>> getSystemConnectionsPaginated(
            @Parameter(description = "ID of the Connector",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("system_id") String systemId,
            @Parameter(description = "The maximum number of System Connections to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(systemConnectionService.getSystemConnectionPaginated(systemId, offset, limit, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets System by given guid.",
            description = "This method can be used to get System by given guid.",
            operationId = "get_system_connection_param_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System connection param has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "System not found with given id"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/params/{param_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"connection_r"}, level = ANY_ROLE)
    public ResponseEntity<SystemConnectionParam> getSystemConnectionParamById(
            @Parameter(description = "ID of the System Connection Param",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("param_id") String paramId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(systemConnectionService.getSystemConnectionParamById(paramId, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets System by given guid.",
            description = "This method can be used to get System by given guid.",
            operationId = "get_system_connection_params_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System connection param has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "System not found with given id"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{system_connection_id}/params", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"connection_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<SystemConnectionParam>> getSystemConnectionParamsPaginated(
            @Parameter(description = "ID of the System Connection",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("system_connection_id") String systemConnectionId,
            @Parameter(description = "The maximum number of System Connection Params to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(systemConnectionService.getSystemConnectionParamsPaginated(systemConnectionId, offset, limit, userDetails), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"connection_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatSystemConnection>> searchQuery(
            @RequestBody SearchRequestWithJoin request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        log.info(request.toString());

        SearchResponse<FlatSystemConnection> res = systemConnectionService.searchSystemConnection(request, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

}
