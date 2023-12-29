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
import ru.bssg.lottabyte.core.model.steward.FlatSteward;
import ru.bssg.lottabyte.core.model.steward.Steward;
import ru.bssg.lottabyte.core.model.steward.UpdatableStewardEntity;
import ru.bssg.lottabyte.core.ui.model.SearchRequest;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.StewardService;

import java.util.Collections;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "Steward", description = "APIs for Steward.")
@RestController
@Slf4j
@RequestMapping("v1/stewards")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class StewardController {
    private final StewardService stewardService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Steward by given guid.",
            description = "This method can be used to get Steward by given guid.",
            operationId = "get_steward_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Steward has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Steward with steward_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{steward_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"st_r"}, level = ANY_ROLE)
    public ResponseEntity<Steward> getStewardById(
            @Parameter(description = "Artifact ID of the Steward",
            example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("steward_id") String stewardId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(stewardService.getStewardById(stewardId, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all Stewards.",
            description = "This method can be used to get all Stewards.",
            operationId = "get_stewards_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stewards have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"st_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<Steward>> getStewardsPaginated(
            @Parameter(description = "The maximum number of Stewards to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(stewardService.getStewardsPaginated(offset, limit, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates Steward.",
            description = "This method can be used to create Steward.",
            operationId = "create_steward"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Steward has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"st_r", "st_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Steward> createSteward(@RequestBody UpdatableStewardEntity newStewardEntity,
                                                 @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(stewardService.createSteward(newStewardEntity, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Updates Steward by given guid.",
            description = "This method can be used to update Steward by given guid.",
            operationId = "patch_steward"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Steward has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Steward with steward_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{steward_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"st_r", "st_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Steward> patchSteward(
            @Parameter(description = "Artifact ID of the Steward",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("steward_id") String stewardId,
            @RequestBody UpdatableStewardEntity stewardEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(stewardService.updateSteward(stewardId, stewardEntity, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Deletes Steward by given guid.",
            description = "This method can be used to delete Steward by given guid.",
            operationId = "delete_steward"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Steward has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Steward with steward_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{steward_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"st_r", "st_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<ArchiveResponse> deleteSteward(
            @Parameter(description = "Artifact ID of the Steward",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("steward_id") String stewardId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        stewardService.deleteStewardById(stewardId, userDetails);
        ArchiveResponse resp = new ArchiveResponse();
        resp.setDeletedGuids(Collections.singletonList(stewardId));
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"st_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatSteward>> searchDomains(
            @RequestBody SearchRequestWithJoin request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        SearchResponse<FlatSteward> res = stewardService.searchStewards(request, userDetails);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

}
