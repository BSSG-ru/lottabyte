package ru.bssg.lottabyte.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=false)
public class FlatWFItemObject extends FlatModeledObject {
    protected String artifactType;
    protected String artifactTypeName;
    protected String workflowState;
    protected String workflowStateName;
    protected String userName;
    protected String workflowTaskId;

    public FlatWFItemObject(FlatWFItemObject f) {
        super(f);
        this.artifactType = f.getArtifactType();
        this.artifactTypeName = f.getArtifactTypeName();
        this.workflowState = f.getWorkflowState();
        this.workflowStateName = f.getWorkflowStateName();
        this.userName = f.getUserName();
        this.workflowTaskId = f.getWorkflowTaskId();
    }
}
