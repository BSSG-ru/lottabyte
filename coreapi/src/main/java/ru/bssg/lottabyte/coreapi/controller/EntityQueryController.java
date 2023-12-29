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
import ru.bssg.lottabyte.core.model.dataasset.DataAsset;
import ru.bssg.lottabyte.core.model.dataentity.FlatDataEntity;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;
import ru.bssg.lottabyte.core.model.entityQuery.FlatEntityQuery;
import ru.bssg.lottabyte.core.model.entityQuery.UpdatableEntityQueryEntity;
import ru.bssg.lottabyte.core.ui.model.SearchRequest;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.EntityQueryService;

import java.util.Collections;
import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "Entity Queries", description = "APIs for Entity Queries")
@RestController
@Slf4j
@RequestMapping("v1/queries")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class EntityQueryController {
    private final EntityQueryService queryService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets EntityQuery by given guid.",
            description = "This method can be used to get EntityQuery by given guid.",
            operationId = "get_query_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "EntityQuery has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "EntityQuery with query_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{query_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"req_r"}, level = ANY_ROLE)
    public ResponseEntity<EntityQuery> getQueryById(
            @Parameter(description = "Artifact ID of the Query",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("query_id") String queryId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(queryService.getEntityQueryById(queryId, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all EntityQueries by given guid.",
            description = "This method can be used to get all EntityQueries by given guid.",
            operationId = "getAllQueriesOfEntityPaginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All EntityQueries has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "EntityQueries with entity_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/entity/{entity_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"req_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<EntityQuery>> getAllQueriesOfEntityPaginated(
            @Parameter(description = "Artifact ID of the Entity",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("entity_id") String entityId,
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(queryService.getAllQueryEntitiesPaginated(entityId, offset, limit, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Create EntityQuery.",
            description = "This method can be used to creating EntityQuery.",
            operationId = "create_query"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "EntityQuery has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"req_r", "req_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<EntityQuery> createQuery(
            @RequestBody UpdatableEntityQueryEntity newEntityQueryEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);
        return new ResponseEntity<>(queryService.createQuery(newEntityQueryEntity, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Update EntityQuery.",
            description = "This method can be used to updating EntityQuery by given guid.",
            operationId = "update_query_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "EntityQuery has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "EntityQuery with query_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{query_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"req_r", "req_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<EntityQuery> patchQuery(
            @Parameter(description = "Artifact ID of the Query",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("query_id") String queryId,
            @RequestBody UpdatableEntityQueryEntity entityQueryEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);
        return new ResponseEntity<>(queryService.patchQuery(queryId, entityQueryEntity, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Delete EntityQuery.",
            description = "This method can be used to deleting EntityQuery by given guid.",
            operationId = "delete_query_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "EntityQuery has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "EntityQuery with query_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{query_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"req_r", "req_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<?> deleteQuery(
            @Parameter(description = "Artifact ID of the Query",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("query_id") String queryId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        EntityQuery result = queryService.deleteQuery(queryId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        if (result == null) {
            ArchiveResponse resp = new ArchiveResponse();
            resp.setDeletedGuids(Collections.singletonList(queryId));
            return ResponseEntity.ok(resp);
        } else {
            return ResponseEntity.ok(result);
        }
    }

    @Hidden
    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"req_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatEntityQuery>> searchQuery(
            @RequestBody SearchRequestWithJoin request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);
        SearchResponse<FlatEntityQuery> res = queryService.searchEntityQuery(request, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/search_by_domain/{domain_id}", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"req_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatEntityQuery>> searchEntityQueryByDomain(
            @RequestBody SearchRequestWithJoin request,
            @PathVariable("domain_id") String domainId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        SearchResponse<FlatEntityQuery> res = queryService.searchEntityQueryByDomain(request, domainId, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @Hidden
    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Get EntityQuery versions.",
            description = "This method can be used to read EntityQuery versions.",
            operationId = "getEntityQueryVersionsById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "EntityQuery versions have been read successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @Secured(roles = {"req_r"}, level = ANY_ROLE)
    @RequestMapping(value = "/{query_id}/versions", method = RequestMethod.GET, produces = { "application/json"})
    public ResponseEntity<PaginatedArtifactList<EntityQuery>> getEntityQueryVersionsById(
            @PathVariable("query_id") String queryId,
            @Parameter(description = "The maximum number of Entity Query versions to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        PaginatedArtifactList<EntityQuery> list = queryService.getEntityQueryVersionsById(queryId, offset, limit, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Entity Query version by given guid and version id.",
            description = "This method can be used to get EntityQuery history version by given guid and version id.",
            operationId = "getEntityQueryVersionById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity Query version has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Entity Query version not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{query_id}/versions/{version_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"req_r"}, level = ANY_ROLE)
    public ResponseEntity<EntityQuery> getEntityQueryVersionById(
            @Parameter(description = "ID of the Entity Query", example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("query_id") String queryId,
            @Parameter(description = "Version ID of the Entity Query", example = "1")
            @PathVariable("version_id") Integer versionId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        return ResponseEntity.ok(queryService.getEntityQueryVersionById(queryId, versionId, jwtHelper.getUserDetail(HttpUtils.getToken(headers))));
    }
}
