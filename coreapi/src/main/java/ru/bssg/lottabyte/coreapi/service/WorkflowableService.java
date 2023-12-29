package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.workflow.WorkflowType;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.WorkflowableRepository;

import java.util.Collections;

@RequiredArgsConstructor
public abstract class WorkflowableService<T extends ModeledObject<? extends Entity>>
        implements IWorkflowableService<T> {

    private final WorkflowableRepository<T> repository;
    private final WorkflowService workflowService;
    private final TagService tagService;
    private final ArtifactType serviceArtifactType;
    private final ElasticsearchService elasticsearchService;

    public boolean existsInState(String artifactId, ArtifactState artifactState,
            UserDetails userDetails) throws LottabyteException {
        T res = repository.getById(artifactId, userDetails);
        WorkflowableMetadata md = (WorkflowableMetadata) res.getMetadata();
        if (!artifactState.equals(md.getState()))
            return false;
        return true;
    }

    public String getDraftArtifactId(String publishedId, UserDetails userDetails) {
        return repository.getDraftId(publishedId, userDetails);
    }

    public String getIdByAncestorDraftId(String ancestorDraftId, UserDetails userDetails) {
        return repository.getIdByAncestorDraftId(ancestorDraftId, userDetails);
    }

    public String createDraft(String publishedId, String draftId, WorkflowState workflowState,
            WorkflowType workflowType, UserDetails userDetails) throws LottabyteException {
        String workflowTaskId = workflowService.getNewWorkflowTaskUUID().toString();
        String newDraftId = repository.createDraftFromPublished(publishedId, draftId, workflowTaskId, userDetails);

        // String draftId = repository.createDraftFromPublished(publishedId,
        // workflowTaskId, userDetails);
        tagService.mergeTags(publishedId, serviceArtifactType, newDraftId, serviceArtifactType, userDetails);
        workflowService.postCreateDraft(workflowTaskId, workflowType, newDraftId, serviceArtifactType, workflowState,
                userDetails);
        return newDraftId;
    }

    public void wfCancel(String draftDataAssetId, UserDetails userDetails) throws LottabyteException {
        T current = repository.getById(draftDataAssetId, userDetails);
        if (current == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftDataAssetId);
        if (!ArtifactState.DRAFT.equals(((WorkflowableMetadata) current.getMetadata()).getState()))
            throw new LottabyteException(
                    Message.LBE03003, userDetails.getLanguage());
        repository.setStateById(draftDataAssetId, ArtifactState.DRAFT_HISTORY, userDetails);
    }

    public void wfApproveRemoval(String draftDataAssetId, UserDetails userDetails) throws LottabyteException {
        T current = repository.getById(draftDataAssetId, userDetails);
        if (current == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftDataAssetId);
        String publishedId = ((WorkflowableMetadata) current.getMetadata()).getPublishedId();
        if (publishedId == null)
            throw new LottabyteException(
                    Message.LBE03006,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftDataAssetId);
        repository.setStateById(current.getId(), ArtifactState.DRAFT_HISTORY, userDetails);
        repository.setStateById(publishedId, ArtifactState.REMOVED, userDetails);
        elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(publishedId), userDetails);
    }

    /*
     * public String createDraft(String publishedId, WorkflowState workflowState,
     * UserDetails userDetails) throws LottabyteException {
     * T current = getById(publishedId, userDetails);
     * String draftId = createDraftDataAsset(current, workflowState, userDetails);
     * return getById(draftId, userDetails);
     * }
     */

}
