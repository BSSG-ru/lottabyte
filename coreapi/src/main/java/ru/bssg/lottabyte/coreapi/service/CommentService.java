package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.comment.Comment;
import ru.bssg.lottabyte.core.model.comment.FlatComment;
import ru.bssg.lottabyte.core.model.comment.UpdatableComment;
import ru.bssg.lottabyte.core.ui.model.SearchColumn;
import ru.bssg.lottabyte.core.ui.model.SearchColumnForJoin;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.CommentRepository;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;

    private final SearchColumn[] searchableColumns = {
            new SearchColumn("comment_text", SearchColumn.ColumnType.Text),
            new SearchColumn("parent_comment_id", SearchColumn.ColumnType.UUID),
            new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
            new SearchColumn("artifact_id", SearchColumn.ColumnType.UUID),
            new SearchColumn("artifact_type", SearchColumn.ColumnType.Text)
    };

    private final SearchColumnForJoin[] joinColumns = {
            new SearchColumnForJoin("domain_id", "system_to_domain", SearchColumn.ColumnType.UUID, "domain_id", "id")
    };

    public void deleteAllCommentsByArtifactId(String artifactId, UserDetails userDetails) {
        commentRepository.deleteAllCommentsByArtifactId(artifactId, userDetails);
    }

    public List<Comment> getCommentsByArtifactId(String artifactId, UserDetails userDetails) throws LottabyteException {
        if(artifactId == null || !commentRepository.existCommentsByArtifactId(artifactId, userDetails)){
            throw new LottabyteException(Message.LBE01701, userDetails.getLanguage(), artifactId);
        }

        return commentRepository.getCommentsByArtifactId(artifactId, userDetails);
    }

    public Comment createComment(UpdatableComment updatableComment, UserDetails userDetails) throws LottabyteException {
        ArtifactType artType = commentRepository.getDataArtifactType(updatableComment.getArtifactType());
        if (updatableComment.getCommentText() == null || updatableComment.getCommentText().isEmpty()) {
            throw new LottabyteException(Message.LBE01702, userDetails.getLanguage());
        }
        if (updatableComment.getArtifactType() == null || artType == null) {
            throw new LottabyteException(Message.LBE01703, userDetails.getLanguage(), updatableComment.getArtifactType());
        }
        if (!commentRepository.existsObjectByIdAndArtifactType(updatableComment.getArtifactId(), updatableComment.getArtifactType(), userDetails)) {
            throw new LottabyteException(Message.LBE01704, userDetails.getLanguage(), updatableComment.getArtifactId(), updatableComment.getArtifactType());
        }
        Comment comment = commentRepository.createComment(updatableComment, userDetails);
        return getCommentById(comment.getId(), userDetails);
    }

    public Comment patchComment(String commentId, UpdatableComment comment, UserDetails userDetails) throws LottabyteException {
        if (comment.getCommentText() == null || comment.getCommentText().isEmpty()) {
            throw new LottabyteException(Message.LBE01702, userDetails.getLanguage());
        }
        if(commentId != null && !commentRepository.existCommentsById(commentId, userDetails)){
            throw new LottabyteException(Message.LBE01705, userDetails.getLanguage(), commentId);
        }
        commentRepository.patchComment(commentId, comment, userDetails);
        return getCommentById(commentId, userDetails);
    }

    public ArchiveResponse deleteComment(String commentId, UserDetails userDetails) throws LottabyteException {
        if(commentId != null && !commentRepository.existCommentsById(commentId, userDetails)){
            throw new LottabyteException(Message.LBE01705, userDetails.getLanguage(), commentId);
        }
        commentRepository.deleteComment(commentId, userDetails);
        ArchiveResponse archiveResponse = new ArchiveResponse();
        archiveResponse.setArchivedGuids(Collections.singletonList(commentId));
        return archiveResponse;
    }
    public Comment getCommentById(String commentId, UserDetails userDetails) {
        return commentRepository.getCommentById(commentId, userDetails);
    }

    public SearchResponse<FlatComment> searchComment(SearchRequestWithJoin request, UserDetails userDetails) throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);

        return commentRepository.searchComment(request, searchableColumns, joinColumns, userDetails);
    }
}
