package ru.bssg.lottabyte.coreapi.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
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
import ru.bssg.lottabyte.core.model.comment.Comment;
import ru.bssg.lottabyte.core.model.comment.FlatComment;
import ru.bssg.lottabyte.core.model.comment.UpdatableComment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.core.usermanagement.security.annotation.Secured;
import ru.bssg.lottabyte.core.util.HttpUtils;
import ru.bssg.lottabyte.coreapi.service.CommentService;

import java.util.List;
import java.util.Objects;

import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ALL_ROLES_STRICT;
import static ru.bssg.lottabyte.core.usermanagement.util.SecurityLevel.ANY_ROLE;

@CrossOrigin
@Tag(name = "Comments", description = "APIs for Comments")
@RestController
@Slf4j
@RequestMapping("v1/comments")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final JwtHelper jwtHelper;

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Gets Comments by given artifact id guid.",
            description = "This method can be used to get Comments by given artifact id guid.",
            operationId = "get_comments_by_artifact_id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment has been retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{artifact_id}", method = RequestMethod.GET, produces = { "application/json"})
    @Secured(roles = {"comments_r"}, level = ANY_ROLE)
    public ResponseEntity<List<Comment>> getCommentsByArtifactId(
            @Parameter(description = "Artifact ID",
                       example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("artifact_id") String artifactId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(commentService.getCommentsByArtifactId(artifactId, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "creates Comment.",
            description = "This method can be used to create Comment.",
            operationId = "create_comment"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment has been created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"comments_r", "comments_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Comment> createComment(
            @RequestBody UpdatableComment comment,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);
        return new ResponseEntity<>(commentService.createComment(comment, userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Updates Comment by given guid.",
            description = "This method can be used to update Comment by given guid.",
            operationId = "update_comment"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment has been updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{comment_id}", method = RequestMethod.PATCH, produces = { "application/json"})
    @Secured(roles = {"comments_r", "comments_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<Comment> patchComment(
            @Parameter(description = "Artifact ID of the Comment",
                       example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("comment_id") String commentId,
            @RequestBody UpdatableComment comment,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(commentService.patchComment(commentId, comment,userDetails), HttpStatus.OK);
    }

    @Operation(
            security = @SecurityRequirement(name = "bearerAuth"),
            summary = "Deletes Comment by given guid.",
            description = "This method can be used to delete Comment by given guid.",
            operationId = "delete_comment"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Domain has been deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server error")
    })
    @RequestMapping(value = "/{comment_id}", method = RequestMethod.DELETE, produces = { "application/json"})
    @Secured(roles = {"comments_r", "comments_u"}, level = ALL_ROLES_STRICT)
    public ResponseEntity<ArchiveResponse> deleteDomain(
            @Parameter(description = "Artifact ID of the Comment",
                    example = "aa0e33f5-3108-4d45-a530-0307458362d4")
            @PathVariable("comment_id") String commentId,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION)).replace("Bearer ","");
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        return new ResponseEntity<>(commentService.deleteComment(commentId, userDetails), HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = { "application/json"})
    @Secured(roles = {"comments_r"}, level = ANY_ROLE)
    public ResponseEntity<SearchResponse<FlatComment>> searchComment(
            @RequestBody SearchRequestWithJoin request,
            @RequestHeader HttpHeaders headers) throws LottabyteException {
        String token = HttpUtils.getToken(headers);
        UserDetails userDetails = jwtHelper.getUserDetail(token);

        SearchResponse<FlatComment> res = commentService.searchComment(request, userDetails);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }
}
