package ru.bssg.lottabyte.coreapi.service;

import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.ArtifactState;
import ru.bssg.lottabyte.core.model.ModeledObject;
import ru.bssg.lottabyte.core.model.WorkflowState;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.workflow.WorkflowType;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;

public interface IWorkflowableService<T extends ModeledObject> {

    public void wfApproveRemoval(String draftId, UserDetails userDetails) throws LottabyteException;

    public void wfCancel(String draftId, UserDetails userDetails) throws LottabyteException;

    public T wfPublish(String draftId, UserDetails userDetails) throws LottabyteException;

    public boolean existsInState(String artifactId, ArtifactState artifactState, UserDetails userDetails)
            throws LottabyteException;

    public String getDraftArtifactId(String publishedId, UserDetails userDetails) throws LottabyteException;

    public String createDraft(String publishedId, WorkflowState workflowState,
            WorkflowType workflowType, UserDetails userDetails) throws LottabyteException;

    public T getById(String id, UserDetails userDetails) throws LottabyteException;

    public String getIdByAncestorDraftId(String ancestorDraftId, UserDetails userDetails) throws LottabyteException;

}
