package ru.bssg.lottabyte.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class WorkflowableMetadata extends Metadata {

    private ArtifactState state;
    private String workflowTaskId;
    private String publishedId;
    private String draftId;
    private Integer versionId;
    private String ancestorDraftId;

    public WorkflowableMetadata(ResultSet rs, ArtifactType artifactType) throws SQLException {
        this.setId(rs.getString("id"));
        this.setCreatedBy(rs.getString("creator"));
        if(rs.getTimestamp("created") != null)
            this.setCreatedAt(rs.getTimestamp("created").toLocalDateTime());
        if(rs.getTimestamp("modified") != null)
            this.setModifiedAt(rs.getTimestamp("modified").toLocalDateTime());
        this.setModifiedBy(rs.getString("modifier"));
        this.setName(rs.getString("name"));
        this.setArtifactType(artifactType.toString());
        try{
            rs.findColumn("history_id");
            if(rs.getObject("history_id") != null)
                this.setHistoryId(rs.getInt("history_id"));
        }catch(SQLException ignored){}
        if(rs.getTimestamp("history_start") != null)
            this.setEffectiveStartDate(rs.getTimestamp("history_start").toLocalDateTime());
        if(rs.getTimestamp("history_end") != null)
            this.setEffectiveEndDate(rs.getTimestamp("history_end").toLocalDateTime());
        try{
            rs.findColumn("state");
            if(rs.getObject("state") != null)
                this.setState(ArtifactState.valueOf(rs.getString("state")));
            rs.findColumn("workflow_task_id");
            if(rs.getObject("workflow_task_id") != null)
                this.setWorkflowTaskId(rs.getString("workflow_task_id"));
            rs.findColumn("published_id");
            if(rs.getObject("published_id") != null)
                this.setPublishedId(rs.getString("published_id"));
            rs.findColumn("state");
            if(rs.getObject("state") != null)
                this.setVersionId(rs.getInt("version_id"));
            rs.findColumn("ancestor_draft_id");
            if(rs.getObject("ancestor_draft_id") != null)
                this.setAncestorDraftId(rs.getString("ancestor_draft_id"));
        }catch(SQLException ignored){}
    }

}
