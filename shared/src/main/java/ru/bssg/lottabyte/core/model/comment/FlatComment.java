package ru.bssg.lottabyte.core.model.comment;

import ru.bssg.lottabyte.core.model.ArtifactType;
import ru.bssg.lottabyte.core.model.FlatModeledObject;

import java.time.LocalDateTime;

public class FlatComment extends FlatModeledObject {
    private String commentText;
    private String parentCommentId;
    private String artifactId;
    private ArtifactType artifactType;
    private String modifier;
    private LocalDateTime created;
    private String creator;

    public FlatComment(Comment c) {
        this.id = c.getId();
        this.name = "-";
        this.description = "-";
        this.modified = c.getModified();
        this.versionId = 0;
        this.num = 0;
        this.commentText = c.getCommentText();
        this.parentCommentId = c.getParentCommentId();
        this.artifactId = c.getArtifactId();
        this.artifactType = c.getArtifactType();
        this.modifier = c.getModifier();
        this.created = c.getCreated();
        this.creator = c.getCreator();
    }
}
