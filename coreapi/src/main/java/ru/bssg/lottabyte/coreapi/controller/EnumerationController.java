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
import ru.bssg.lottabyte.core.model.enumeration.Enumeration;
import ru.bssg.lottabyte.core.model.enumeration.FlatEnumeration;
import ru.bssg.lottabyte.core.model.enumeration.UpdatableEnumerationEntity;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.EnumerationService;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "Enumeration", description = "APIs for Enumeration Entity.")
@RestController
@Slf4j
@RequestMapping("v1/enumeration")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class EnumerationController {
    private final EnumerationService enumerationService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Enumeration Entity by given guid.",
            description = "This method can be used to get Enumeration by given guid.",
            operationId = "getEnumerationById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Enumeration Entity has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Enumeration Entity with specified id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{enumeration_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"enumeration_r"}, level = ANY_ROLE)
    public ResponseEntity<Enumeration> getEnumerationById(
            @Parameter(description = "Artifact ID of the Enumeration Entity",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("enumeration_id") String enumerationId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(enumerationService.getEnumerationById(enumerationId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all Enumeration Entities paginated.",
            description = "This method can be used to get all Enumeration with paging.",
            operationId = "getEnumerationPaginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Enumeration Entities have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"enumeration_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<Enumeration>> getEnumerationPaginated(
            @Parameter(description = "The maximum number of Enumeration Entities to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {

        return new ResponseEntity<>(enumerationService.getEnumerationPaginated(offset, limit, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates new Enumeration Entity.",
            description = "This method can be used to create Enumeration.",
            operationId = "createEnumeration"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Business Entity has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"enumeration_r", "enumeration_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Enumeration> createEnumeration(
            @RequestBody UpdatableEnumerationEntity newEnumerationEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(enumerationService.createEnumeration(newEnumerationEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Updates Enumeration Entity by given guid.",
            description = "This method can be used to update Enumeration.",
            operationId = "patchEnumeration"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Enumeration has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Enumeration with specified id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{enumeration_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"enumeration_r", "enumeration_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Enumeration> patchEnumeration(
            @Parameter(description = "Artifact ID of the Business Entity to be patched",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("enumeration_id") String enumerationId,
            @RequestBody UpdatableEnumerationEntity enumerationEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(enumerationService.patchEnumeration(enumerationId, enumerationEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Deletes Enumeration Entity by given guid.",
            description = "This method can be used to delete Enumeration by id.",
            operationId = "deleteEnumeration"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Enumeration has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Enumeration with specified id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{enumeration_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"enumeration_r", "enumeration_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<ArchiveResponse> deleteEnumeration(
            @Parameter(description = "Artifact ID of the Enumeration Entity to be deleted",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("enumeration_id") String enumerationId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(enumerationService.deleteEnumeration(enumerationId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json"})
    public ResponseEntity<SearchResponse<FlatEnumeration>> searchEnumeration(
            @RequestBody SearchRequestWithJoin request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(enumerationService.searchEnumeration(request, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/{enumeration_id}/versions", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"enumeration_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<Enumeration>> getEnumerationVersions(
            @PathVariable("enumeration_id") String enumerationId,
            @Parameter(description = "The maximum number of Enumeration Entity versions to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return new ResponseEntity<>(enumerationService.getEnumerationVersions(enumerationId,
                offset, limit, jwtHelper.getUserDetail(HttpUtils.getToken(headers))), HttpStatus.OK);
    }

}
