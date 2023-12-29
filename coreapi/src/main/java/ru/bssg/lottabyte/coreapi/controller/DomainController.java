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
import ru.bssg.lottabyte.core.model.FlatModeledObject;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.domain.FlatDomain;
import ru.bssg.lottabyte.core.model.domain.UpdatableDomainEntity;
import ru.bssg.lottabyte.core.ui.model.SearchRequest;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.DomainService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "Domain", description = "APIs for Domain.")
@RestController
@Slf4j
@RequestMapping("v1/domains")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)

public class DomainController {
    private final DomainService domainService;
    private JwtHelper jwtHelper;


    @Autowired
    public DomainController(DomainService domainService, JwtHelper jwtHelper) {
        this.domainService = domainService;
        this.jwtHelper = jwtHelper;
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Domain by given guid.",
            description = "This method can be used to get Domain by given guid.",
            operationId = "getDomainById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Domain has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Domain not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{domain_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"domain_r"}, level = ANY_ROLE)
    public ResponseEntity<Domain> getDomainById(
            @Parameter(description = "ID of the Domain",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("domain_id") String domainId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        Domain d = domainService.getDomainById(domainId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(d, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Domain version by given guid and version id.",
            description = "This method can be used to get Domain history version by given guid and version id.",
            operationId = "getDomainVersionById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Domain has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Domain version not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{domain_id}/versions/{version_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"domain_r"}, level = ANY_ROLE)
    public ResponseEntity<Domain> getDomainVersionById(
            @Parameter(description = "ID of the Domain",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("domain_id") String domainId,
            @Parameter(description = "Version ID of the Domain",
                    example = "1")
            @PathVariable("version_id") Integer versionId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        Domain d = domainService.getDomainVersionById(domainId, versionId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(d, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Domain versions list by domain guid.",
            description = "This method can be used to get Domain history versions by given guid.",
            operationId = "getDomainVersionsById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Domain versions have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{domain_id}/versions", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"domain_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<Domain>> getDomainVersionsById(
            @PathVariable("domain_id") String domainId,
            @Parameter(description = "The maximum number of Domain versions to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        PaginatedArtifactList<Domain> list = domainService.getDomainVersions(domainId, offset, limit, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets paginated Domains.",
            description = "This method can be used to get all Domains with paging.",
            operationId = "getDomainsPaginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Domains have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Domains not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"domain_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<Domain>> getDomainsPaginated(
            @Parameter(description = "The maximum number of Domains to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @Parameter(description = "Artifact state.")
            @RequestParam(value="state", defaultValue = "PUBLISHED") String artifactState,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        PaginatedArtifactList<Domain> list = domainService.getDomainsPaginated(offset, limit, artifactState, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates Domain.",
            description = "This method can be used to create Domain.",
            operationId = "createDomain"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Domain has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"domain_r", "domain_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Domain> createDomain(@RequestBody UpdatableDomainEntity newDomainEntity,
                                               @RequestHeader HttpHeaders headers) throws LottabyteException {
        Domain d = domainService.createDomain(newDomainEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(d, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Updates Domain by given guid.",
            description = "This method can be used to update Domain by given guid.",
            operationId = "patchDomain"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Domain has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{domain_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"domain_r", "domain_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Domain> patchDomain(
            @Parameter(description = "ID of the Domain",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("domain_id") String domainId,
            @RequestBody UpdatableDomainEntity domainEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        Domain d = domainService.updateDomain(domainId, domainEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(d, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Deletes Domain by given guid.",
            description = "This method can be used to delete Domain by given guid.",
            operationId = "get_custom_attribute_definition_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Domain has been deleted successfully for DRAFT domain. Domain draft marked for removal has been created for PUBLISHED domain"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{domain_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"domain_r", "domain_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<?> deleteDomain(
            @Parameter(description = "ID of the Domain",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("domain_id") String domainId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        Domain result = domainService.deleteDomainById(domainId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        if (result == null) {
            ArchiveResponse resp = new ArchiveResponse();
            resp.setDeletedGuids(Collections.singletonList(domainId));
            return ResponseEntity.ok(resp);
        } else {
            return ResponseEntity.ok(result);
        }
    }

    @Hidden
    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"domain_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatDomain>> searchDomains(
            @RequestBody SearchRequestWithJoin request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        return ResponseEntity.ok(domainService.searchDomains(request, jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
    }


}
