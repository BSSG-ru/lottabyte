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
import ru.bssg.lottabyte.core.model.recentView.RecentView;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.RecentViewService;

import java.util.List;
import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "RecentViews", description = "APIs for RecentViews")
@RestController
@Slf4j
@RequestMapping("v1/recent_views")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class RecentViewController {
    private final RecentViewService recentViewService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Updates recent views",
            description = "This method can be used to update recent views.",
            operationId = "update_recent_view"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ResentViews were updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{artifact_type}/{artifact_id}", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"recent_views_r"}, level = ANY_ROLE)
    public ResponseEntity<RecentView> changeRecentView(
            @Parameter(description = "Artifact ID of the artifact",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("artifact_id") String artifactId,
            @Parameter(description = "Artifact type",
                    example = "entity")
            @PathVariable("artifact_type") String artifactType,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return new ResponseEntity<>(recentViewService.changeRecentView(artifactId, artifactType,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Get recent views",
            description = "This method can be used to get recent views.",
            operationId = "get_recent_views"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ResentViews were retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"recent_views_r"}, level = ANY_ROLE)
    public ResponseEntity<List<RecentView>> getRecentViews(
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return new ResponseEntity<>(recentViewService.getRecentViews(null,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Get recent views by artifact type",
            description = "This method can be used to get recent views with artifact type filter.",
            operationId = "getRecentViewsByArtifactType"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ResentViews were retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{artifact_type}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"recent_views_r"}, level = ANY_ROLE)
    public ResponseEntity<List<RecentView>> getRecentViewsByArtifactType(
            @Parameter(description = "Artifact type",
                    example = "entity")
            @PathVariable("artifact_type") String artifactType,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return new ResponseEntity<>(recentViewService.getRecentViews(artifactType,
                jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }
}
