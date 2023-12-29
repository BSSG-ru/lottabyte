package ru.bssg.lottabyte.coreapi.controller;

import io.swagger.v3.oas.annotations.Hidden;
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
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.tag.*;
import ru.bssg.lottabyte.core.ui.model.SearchRequest;
import ru.bssg.lottabyte.core.ui.model.SimpleSearchRequest;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.TagCategoryService;
import ru.bssg.lottabyte.coreapi.service.TagService;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tag", description = "APIs for Tag.")
@RestController
@Slf4j
@RequestMapping("v1/tags")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;
    private final TagCategoryService tagCategoryService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all Artifact Tags.",
            description = "This method can be used to get all Artifact Tags.",
            operationId = "get_artifact_tags_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Artifact Tags have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Artifact Tags with artifact_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/artifacts/{artifact_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"tag_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<Tag>> getArtifactTagsPaginated(
            @Parameter(description = "Artifact ID of the Artifact",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("artifact_id") String artifactId,
            @Parameter(description = "The maximum number of Artifact Tags to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        PaginatedArtifactList<Tag> list = tagService.getArtifactTagsPaginated(artifactId, offset, limit, userDetails);
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all Tags.",
            description = "This method can be used to get all Tags.",
            operationId = "get_tags_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tags have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Tags with artifact_id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"tag_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<Tag>> getTagsPaginated(
            @Parameter(description = "The maximum number of Tags to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        PaginatedArtifactList<Tag> list = tagService.getTagsPaginated(offset, limit, userDetails);
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Searc Tags.",
            description = "This method can be used to search Tags.",
            operationId = "searchTags"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tags have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/search", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"tag_r"}, level = ANY_ROLE)
    public ResponseEntity<List<FlatTag>> searchTags(
            @Parameter(description = "Text to search in tags name")
            @RequestParam(value="query", defaultValue = "") String query,
            @Parameter(description = "The maximum number of Tags to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,

            @RequestHeader HttpHeaders headers) throws LottabyteException {

        List<FlatTag> list = tagService.searchTags(query, offset, limit, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Searc Tags.",
            description = "This method can be used to search Tags.",
            operationId = "searchTags2"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tags have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/search2", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"tag_r"}, level = ANY_ROLE)
    public ResponseEntity<List<FlatTag>> searchTags2(
            @RequestBody SimpleSearchRequest request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        List<FlatTag> list = tagService.searchTags(request.getQuery(), request.getOffset(), request.getLimit(), jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Tag by given guid.",
            description = "This method can be used to get Tag by given guid.",
            operationId = "get_tag_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Tag with given id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{tag_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"tag_r"}, level = ANY_ROLE)
    public ResponseEntity<Tag> getTagById(
            @Parameter(description = "Artifact ID of the Tag",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("tag_id") String tagId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        Tag tag = tagService.getTagById(tagId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(tag, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates Tag.",
            description = "This method can be used to create Tag.",
            operationId = "create_tag"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"tag_r", "tag_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Tag> createTag(@RequestBody UpdatableTagEntity newTagEntity,
                                         @RequestHeader HttpHeaders headers) throws LottabyteException {

        Tag tag = tagService.createTag(newTagEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(tag, HttpStatus.OK);

    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Updates Tag by given guid.",
            description = "This method can be used to update Tag by given guid.",
            operationId = "patch_tag"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Tag with given id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{tag_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"tag_r", "tag_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Tag> patchTag(
            @Parameter(description = "Artifact ID of the Tag",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("tag_id") String tagId,
            @RequestBody UpdatableTagEntity tagEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        Tag tag = tagService.updateTag(tagId, tagEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(tag, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Deletes Tag by given guid.",
            description = "This method can be used to delete Tag by given guid.",
            operationId = "delete_tag"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Tag with given id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{tag_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"tag_r", "tag_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<ArchiveResponse> deleteTag(
            @Parameter(description = "Artifact ID of the Tag",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("tag_id") String tagId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        tagService.deleteTagById(tagId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        ArchiveResponse resp = new ArchiveResponse();
        resp.setDeletedGuids(Collections.singletonList(tagId));
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Tag Category by given guid.",
            description = "This method can be used to get Tag Category by given guid.",
            operationId = "get_tag_category_by_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag Category has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Tag category with given id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/category/{tag_category_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"tag_r"}, level = ANY_ROLE)
    public ResponseEntity<TagCategory> getTagCategoryById(
            @Parameter(description = "Artifact ID of the Tag Category",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("tag_category_id") String tagCategoryId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        TagCategory tc = tagCategoryService.getTagCategoryById(tagCategoryId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(tc, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets all Tag Categories.",
            description = "This method can be used to get all Tag Categories.",
            operationId = "get_tag_categories_paginated"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag Categories have been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/category", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"tag_r"}, level = ANY_ROLE)
    public ResponseEntity<PaginatedArtifactList<TagCategory>> getTagCategoriesPaginated(
            @Parameter(description = "The maximum number of Tag Categories to return - must be at least 1 and cannot exceed 200. The default value is 10.")
            @RequestParam(value="limit", defaultValue = "10") Integer limit,
            @Parameter(description = "Index of the beginning of the page. At present, the offset value can be 0 (zero) or a multiple of limit value.")
            @RequestParam(value="offset", defaultValue = "0") Integer offset,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        PaginatedArtifactList<TagCategory> list = tagCategoryService.getTagCategoriesPaginated(offset, limit, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Creates Tag Category.",
            description = "This method can be used to create Tag Category.",
            operationId = "create_tag_category"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag Category has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/category", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"tag_r", "tag_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<TagCategory> createTagCategory(@RequestBody UpdatableTagCategoryEntity newTagCategoryEntity,
                                                         @RequestHeader HttpHeaders headers) throws LottabyteException {
        TagCategory tc = tagCategoryService.createTagCategory(newTagCategoryEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(tc, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Updates Tag Category by given guid.",
            description = "This method can be used to update Tag Category by given guid.",
            operationId = "patch_tag_category"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag Category has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Tag category with given id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/category/{tag_category_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"tag_r", "tag_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<TagCategory> patchTagCategory(
            @Parameter(description = "Artifact ID of the Tag Category",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("tag_category_id") String tagCategoryId,
            @RequestBody UpdatableTagCategoryEntity tagCategoryEntity,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        TagCategory tc = tagCategoryService.updateTagCategory(tagCategoryId, tagCategoryEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        return new ResponseEntity<>(tc, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Deletes Tag Category by given guid.",
            description = "This method can be used to delete Tag Category by given guid.",
            operationId = "delete_tag_category"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag Category has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Tag category with given id not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/category/{tag_category_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"tag_r", "tag_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<ArchiveResponse> deleteTagCategory(
            @Parameter(description = "Artifact ID of the Tag Category",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("tag_category_id") String tagCategoryId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {

        tagCategoryService.deleteTagCategoryById(tagCategoryId, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));
        ArchiveResponse resp = new ArchiveResponse();
        resp.setDeletedGuids(Collections.singletonList(tagCategoryId));
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Links tag with specified name to artifact, creates tag if not exists by name",
            description = "Links tag with specified name to artifact, creates tag if not exists by name",
            operationId = "link_tag_to_artifact"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag has been linked successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value="/linkToArtifact/{artifact_type}/{artifact_id}", method = RequestMethod.POST, produces = { "application/json" })
    @Secured(roles = {"tag_r", "tag_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Boolean> linkTagToArtifact(
            @Parameter(description = "Artifact ID",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("artifact_id") String artifactId,
            @Parameter(description = "Artifact Type",
                    example = "domain")
            @PathVariable("artifact_type") String artifactType,
            @RequestBody TagEntity tagEntity,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {

        tagService.linkToArtifact(artifactId, artifactType, tagEntity, jwtHelper.getUserDetail(HttpUtils.getToken(headers)));

        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Unlinks tag with specified name to artifact",
            description = "Unlinks tag with specified name to artifact",
            operationId = "unlink_tag_to_artifact"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The tag was unlinked successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value="/unlinkFromArtifact/{artifact_type}/{artifact_id}", method = RequestMethod.POST, produces = { "application/json" })
    @Secured(roles = {"tag_r", "tag_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Boolean> unlinkTagFromArtifact(
            @Parameter(description = "Artifact ID",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("artifact_id") String artifactId,
            @Parameter(description = "Artifact Type",
                    example = "domain")
            @PathVariable("artifact_type") String artifactType,
            @RequestBody TagEntity tagEntity,
            @RequestHeader HttpHeaders headers
    ) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        tagService.unlinkFromArtifact(artifactId, artifactType, tagEntity, userDetails);
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

}
