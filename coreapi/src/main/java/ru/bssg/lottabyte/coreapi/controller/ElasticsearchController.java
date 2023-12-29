package ru.bssg.lottabyte.coreapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactState;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@RestController
@Slf4j
@RequestMapping("/v1/search")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class ElasticsearchController {
    private final ElasticsearchService elasticsearchService;
    private final JwtHelper jwtHelper;
    private final DomainService domainService;
    private final StewardService stewardService;
    private final DataAssetService dataAssetService;
    private final ProductService productService;
    private final EntitySampleService entitySampleService;
    private final SystemService systemService;
    private final TagService tagService;
    private final EntityService entityService;
    private final EntityQueryService queryService;
    private final IndicatorService indicatorService;
    private final BusinessEntityService businessEntityService;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            description = "This method for queries in ElasticSearch.",
            operationId = "search_post"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"elastic_search_r", "elastic_search_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<List<HashMap<String, Object>>> searchPost(
            @Parameter(description = "Elasticsearch query, written by yourself",example = "" +
                    "{\n" +
                    "\"size\": 150,\n" +
                    "    \"_source\": [\n" +
                    "        \"id\",\n" +
                    "        \"artifact_id\"\n" +
                    "    ],\n" +
                    "    \"query\": {\n" +
                    "        \"bool\": {\n" +
                    "            \"must\": [\n" +
                    "                {\n" +
                    "                    \"match\": {\n" +
                    "                        \"name\": \"Доля отгрузок без ЭВСД (SLED13)\"\n" +
                    "                    }\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"match\": {\n" +
                    "\t\t\t\t\t\t\"artifact_type\": \"entity\"\n" +
                    "\t\t\t\t\t}\n" +
                    "                }\n" +
                    "            ]\n" +
                    "        }\n" +
                    "    }\n" +
                    "}")
            @RequestBody String query,
            @RequestHeader HttpHeaders headers
    ) throws Exception {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetail = jwtHelper.getUserDetail(token);
        return new ResponseEntity<>(elasticsearchService.searchPost(query, userDetail), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            description = "method for filling all entities from the database into ElasticSearch.",
            operationId = "insert_searchable_artifacts"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity have been added successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/insertAll", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"elastic_search_r", "elastic_search_u"}, level = ALL_ROLES_STRICT)
    public void insertAllSearchableArtifact(
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetail = jwtHelper.getUserDetail(token);

        List<SearchableArtifact> searchableArtifactList = new ArrayList<>();

        domainService.getDomainsPaginated(0, 10000, ArtifactState.PUBLISHED.name(), userDetail).getResources()
                .forEach(x -> searchableArtifactList.add(domainService.getSearchableArtifact(x, userDetail)));
        systemService.getSystemsPaginated(0, 10000, ArtifactState.PUBLISHED.name(), userDetail).getResources()
                .forEach(x -> searchableArtifactList.add(systemService.getSearchableArtifact(x, userDetail)));
        stewardService.getStewardsPaginated(0, 10000, userDetail).getResources()
                .forEach(x -> searchableArtifactList.add(stewardService.getSearchableArtifact(x, userDetail)));
        entityService.getAllEntitiesPaginated(0, 10000, ArtifactState.PUBLISHED.name(), userDetail).getResources()
                .forEach(x -> searchableArtifactList.add(entityService.getSearchableArtifact(x, userDetail)));
        entitySampleService.getEntitySampleWithPaging(0, 10000, true, userDetail).getResources()
                .forEach(x -> searchableArtifactList.add(entitySampleService.getSearchableArtifact(x, userDetail)));
        queryService.getAllEntityQueriesPaginated(0, 10000, ArtifactState.PUBLISHED.name(), userDetail).getResources()
                .forEach(x -> {
                    try {
                        searchableArtifactList.add(queryService.getSearchableArtifact(x, userDetail));
                    } catch (LottabyteException e) {
                        log.error(e.getMessage(), e);
                    }
                });
        indicatorService.getIndicatorsPaginated(0, 10000, ArtifactState.PUBLISHED.name(), userDetail).getResources()
                .forEach(x -> searchableArtifactList.add(indicatorService.getSearchableArtifact(x, userDetail)));
        businessEntityService.getBusinessEntitiesPaginated(0, 10000, ArtifactState.PUBLISHED.name(), userDetail).getResources()
                .forEach(x -> searchableArtifactList.add(businessEntityService.getSearchableArtifact(x, userDetail)));
        dataAssetService.getAllDataAssetPaginated(0, 10000, ArtifactState.PUBLISHED.name(), userDetail).getResources()
                .forEach(x -> {
                    try {
                        searchableArtifactList.add(dataAssetService.getSearchableArtifact(x, userDetail));
                    } catch (LottabyteException e) {
                        log.error(e.getMessage(), e);
                    }
                });
        productService.getAllProductsPaginated(0, 10000, ArtifactState.PUBLISHED.name(), userDetail).getResources()
                .forEach(x -> searchableArtifactList.add(productService.getSearchableArtifact(x, userDetail)));
        
        elasticsearchService.insertElasticSearchEntity(searchableArtifactList, userDetail);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "This method for queries in ElasticSearch.",
            operationId = "search_get"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"elastic_search_r"}, level = ANY_ROLE)
    public ResponseEntity<String> searchGet(
            @Parameter(description = "Elasticsearch query, written by yourself",example = "q=dc15e377-b636-450f-995f-72d487dcd236")
            @RequestParam(value="query", defaultValue = "q=dc15e377-b636-450f-995f-72d487dcd236") String query,
            @RequestHeader HttpHeaders headers
    ) throws Exception {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetail = jwtHelper.getUserDetail(token);
        return new ResponseEntity<>(elasticsearchService.searchGet(query, userDetail), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "This method for queries in ElasticSearch according to certain criteria",
            operationId = "search_by_name_and_type"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/byNameAndType/{type}/{name}",method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"elastic_search_r", "elastic_search_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<List<HashMap<String, Object>>> searchByNameAndType(
            @Parameter(description = "Artifact type",
                    example = "category")
            @PathVariable("type") String type,
            @Parameter(description = "Artifact name",
                    example = "Надежность")
            @PathVariable("name") String name,
            @RequestHeader HttpHeaders headers
    ) throws Exception {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetail = jwtHelper.getUserDetail(token);
        String query = "{\"_source\": [ \"*\" ], \"query\": { \"bool\": { \"must\": [ { \"wildcard\": { \"metadata.name\": \"*" + name + "*\" } }, {\"match\":{\"metadata.artifact_type\":\"" + type + "\"}} ] } }}";
        return new ResponseEntity<>(elasticsearchService.searchPost(query, userDetail), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            description = "Method for removing all entities from the ElasticSearch index.",
            operationId = "delete_documents_from_index_elastic_search"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity  has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/all", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"elastic_search_r", "elastic_search_u"}, level = ALL_ROLES_STRICT)
    public void deleteAllDocumentFromIndexElasticSearch(
            @Parameter(description = "indexName",example = "category")
            @RequestParam(value="indexName", defaultValue = "category") String indexName,
            @RequestHeader HttpHeaders headers
    ) throws Exception {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetail = jwtHelper.getUserDetail(token);
        elasticsearchService.deleteAllDocumentFromIndexElasticSearch(indexName, userDetail);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            description = "Method for adding records to ElasticSearch.",
            operationId = "insert_elastic_search_entity"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stewards have been added successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/insert", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"elastic_search_r", "elastic_search_u"}, level = ALL_ROLES_STRICT)
    public void insertElasticSearchEntity(
            @Parameter(description = "searchableArtifactList")
            @RequestBody List<SearchableArtifact> searchableArtifactList,
            @RequestHeader HttpHeaders headers
    ) throws Exception {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetail = jwtHelper.getUserDetail(token);
        elasticsearchService.insertElasticSearchEntity(searchableArtifactList, userDetail);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Method for updating records in ElasticSearch",
            operationId = "update_elastic_search_entity"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"elastic_search_r", "elastic_search_u"}, level = ALL_ROLES_STRICT)
    public void updateElasticSearchEntity(
            @Parameter(description = "searchableArtifactList")
            @RequestBody List<SearchableArtifact> searchableArtifactList,
            @RequestHeader HttpHeaders headers
    ) throws Exception {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetail = jwtHelper.getUserDetail(token);
        elasticsearchService.updateElasticSearchEntity(searchableArtifactList, userDetail);
    }
}
