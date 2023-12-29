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
import ru.bssg.lottabyte.core.model.rating.Rating;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.coreapi.service.RatingService;

import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "Ratings", description = "APIs for Ratings")
@RestController
@Slf4j
@RequestMapping("v1/ratings")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class RatingController {
    private final RatingService ratingService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Retrieves rating for artifact by given artifact id guid",
            description = "This method can be used to get rating for artifact by given artifact id guid.",
            operationId = "get_artifact_rating_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rating has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{artifact_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"rating_r"}, level = ANY_ROLE)
    public ResponseEntity<Rating> getArtifactRatingById(
            @Parameter(description = "Artifact ID of the artifact",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("artifact_id") String artifactId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);
        return new ResponseEntity<>(ratingService.getArtifactRatingById(artifactId, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Rates artifact",
            description = "This method can be used to rate Artifact.",
            operationId = "rate_artifact"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rating has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{artifact_type}/{artifact_id}/{rating}", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"rating_r", "rating_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Rating> rateArtifact(
            @Parameter(description = "Artifact ID of the artifact",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("artifact_type") String artifactType,
            @Parameter(description = "Artifact type",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("artifact_id") String artifactId,
            @Parameter(description = "Rating. Can be from 1 to 5",
                    example = "4")
            @PathVariable("rating") Integer rating,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);
        return new ResponseEntity<>(ratingService.rateArtifact(artifactType, artifactId, rating, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "deletes Rating.",
            description = "This method can be used to delete Rating.",
            operationId = "delete_rating"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rating has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{artifact_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"rating_r", "rating_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Rating> removeArtifactRate(
            @Parameter(description = "Artifact ID of the Artifact",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("artifact_id") String artifactId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);
        return new ResponseEntity<>(ratingService.removeArtifactRate(artifactId, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "retrieves own Rating for artifact.",
            description = "This method can be used to get own Rating.",
            operationId = "get_own_rating"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rating has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{artifact_id}/own", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"rating_r", "rating_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Integer> getOwnArtifactRate(
            @Parameter(description = "Artifact ID of the Artifact",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("artifact_id") String artifactId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);
        return new ResponseEntity<>(ratingService.getOwnArtifactRate(artifactId, userDetails), HttpStatus.OK);
    }

}
