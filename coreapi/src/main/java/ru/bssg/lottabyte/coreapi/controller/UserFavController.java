package ru.bssg.lottabyte.coreapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.userfav.UserFav;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.coreapi.service.APILogService;
import ru.bssg.lottabyte.coreapi.service.UserFavService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "User Favs", description = "APIs for User Favs")
@RestController
@Slf4j
@RequestMapping("v1/user_fav")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class UserFavController {
    private final JwtHelper jwtHelper;
    private final UserFavService userFavService;
    private final APILogService apiLogService;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets favs by given user.",
            description = "This method can be used to get favs",
            operationId = "get_user_favs"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Favs has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/get_favs", method = RequestMethod.GET, produces = { "application/json"})
    //@Secured(roles = {"active_r"}, level = ANY_ROLE)
    public ResponseEntity<List<UserFav>> getUserFavs(
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request);
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(userFavService.getUserFavs(null, userDetails), HttpStatus.OK);
    }

    @RequestMapping(value = "/get_favs/{artifact_type}", method = RequestMethod.GET, produces = { "application/json"})
    //@Secured(roles = {"active_r"}, level = ANY_ROLE)
    public ResponseEntity<List<UserFav>> getUserFavs(
            @PathVariable("artifact_type") String artifactType,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, artifactType);
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(userFavService.getUserFavs(artifactType, userDetails), HttpStatus.OK);
    }

    @RequestMapping(value = "/is_in_fav/{artifact_id}", method = RequestMethod.GET, produces = { "application/json"})
    public ResponseEntity<Boolean> getIsInFav(
            @PathVariable("artifact_id") String artifactId,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, artifactId);
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(userFavService.isInFav(artifactId, userDetails), HttpStatus.OK);
    }

    @RequestMapping(value = "/add_to_fav/{artifact_type}/{artifact_id}", method = RequestMethod.GET, produces = { "application/json"})
    public ResponseEntity<Boolean> addToFav(
            @PathVariable("artifact_type") String artifactType,
            @PathVariable("artifact_id") String artifactId,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, artifactType, artifactId);
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        userFavService.addUserFav(artifactId, artifactType, userDetails);

        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @RequestMapping(value = "/del_from_fav/{artifact_id}", method = RequestMethod.GET, produces = { "application/json"})
    public ResponseEntity<Boolean> delFromFav(
            @PathVariable("artifact_id") String artifactId,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) throws LottabyteException {
        apiLogService.logApiCall(request, artifactId);
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        userFavService.delUserFav(artifactId, userDetails);

        return new ResponseEntity<>(true, HttpStatus.OK);
    }
}
