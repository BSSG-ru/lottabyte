package ru.bssg.lottabyte.coreapi.service;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.artifact.Artifact;
import ru.bssg.lottabyte.core.model.etl.ETL;
import ru.bssg.lottabyte.core.model.etl.FlatETL;
import ru.bssg.lottabyte.core.model.etl.SearchableETL;
import ru.bssg.lottabyte.core.model.etl.UpdatableETLEntity;
import ru.bssg.lottabyte.core.model.reference.Reference;
import ru.bssg.lottabyte.core.model.reference.ReferenceEntity;
import ru.bssg.lottabyte.core.model.reference.ReferenceType;
import ru.bssg.lottabyte.core.model.reference.UpdatableReferenceEntity;
import ru.bssg.lottabyte.core.model.relation.Relation;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.model.workflow.WorkflowTask;
import ru.bssg.lottabyte.core.model.workflow.WorkflowType;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.*;
import ru.bssg.lottabyte.coreapi.util.Helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ETLService extends WorkflowableService<ETL> {
    private final ArtifactType serviceArtifactType = ArtifactType.etl;
    private final UserFavService userFavService;
    private final WorkflowService workflowService;
    private final ReferenceService referenceService;
    private final TagService tagService;
    private final ETLRepository etlRepository;
    private final ElasticsearchService elasticsearchService;
    private final SystemRepository systemRepository;

    private final SearchColumn[] searchableColumns = {
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            new SearchColumn("short_description", SearchColumn.ColumnType.Text),
            new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
            new SearchColumn("code", SearchColumn.ColumnType.Text),
            new SearchColumn("algorithm", SearchColumn.ColumnType.Text),
            new SearchColumn("tags", SearchColumn.ColumnType.Text),
            new SearchColumn("system.name", SearchColumn.ColumnType.Text),
            new SearchColumn("etl_type.name", SearchColumn.ColumnType.Text),
            new SearchColumn("version_id", SearchColumn.ColumnType.Number),
    };

    private final SearchColumnForJoin[] joinColumns = {
    };

    @Autowired
    public ETLService(UserFavService userFavService, WorkflowService workflowService,
                      ReferenceService referenceService, TagService tagService, ETLRepository etlRepository,
                      ElasticsearchService elasticsearchService, SystemRepository systemRepository) {
        super(etlRepository, workflowService, tagService, ArtifactType.etl, elasticsearchService, referenceService);
        this.elasticsearchService = elasticsearchService;
        this.tagService = tagService;
        this.workflowService = workflowService;
        this.referenceService = referenceService;
        this.userFavService = userFavService;
        this.etlRepository = etlRepository;
        this.systemRepository = systemRepository;
    }

    public ETL wfSend(String draftId, UserDetails userDetails) throws LottabyteException {
        ETL draft = etlRepository.getById(draftId, userDetails);
        if (draft == null)
            throw new LottabyteException(
                    Message.LBE03004,
                    userDetails.getLanguage(),
                    serviceArtifactType, draftId);

        return draft;
    }

    public ETL getETLById(String etlId, UserDetails userDetails) throws LottabyteException {
        ETL etl = etlRepository.getById(etlId, userDetails);
        if (etl == null)
            throw new LottabyteException(Message.LBE06401,
                    userDetails.getLanguage(), etlId);



        List<Reference> referenceForSource = referenceService.getAllReferenceByTargetIdAndRefType(etlId,
                ReferenceType.SOURCE_TO_ETL, userDetails);
        etl.getEntity().setSourceIds(referenceForSource.stream().map(r ->
                new Artifact(r.getEntity().getSourceId(), r.getEntity().getSourceType())
            ).collect(Collectors.toList()));

        List<Reference> referenceForTarget = referenceService.getAllReferenceBySourceIdAndRefType(etlId,
                ReferenceType.ETL_TO_TARGET, userDetails);
        etl.getEntity().setTargetIds(referenceForTarget.stream().map(r ->
                new Artifact(r.getEntity().getTargetId(), r.getEntity().getTargetType())
        ).collect(Collectors.toList()));

        List<Reference> referenceForBE = referenceService.getAllReferenceBySourceIdAndRefType(etlId,
                ReferenceType.ETL_TO_BUSINESS_ENTITY, userDetails);
        etl.getEntity().setBusinessEntityIds(referenceForBE.stream().map(r ->
                r.getEntity().getTargetId()
        ).collect(Collectors.toList()));


        etl.getMetadata().setTags(tagService.getArtifactTags(etlId, userDetails));

        WorkflowableMetadata md = (WorkflowableMetadata) etl.getMetadata();
        if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
            md.setDraftId(etlRepository.getDraftId(md.getId(), userDetails));
        return etl;
    }

    public void updateReferenceForSource(List<Artifact> sourceIds, String newETLId, String publishedId,
                                         UserDetails userDetails) throws LottabyteException {
        if (sourceIds != null && !sourceIds.isEmpty()) {
            for (Artifact a : sourceIds) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(a.getId());
                referenceEntity.setTargetId(newETLId);
                referenceEntity.setSourceType(a.getArtifactType());
                referenceEntity.setPublishedId(publishedId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.patchReferenceBySourceIdAndTargetId(newReferenceEntity, userDetails);
            }
        }
    }

    public void createReferenceForSource(List<Artifact> sourceIds, String newETLId, String publishedId,
                                         UserDetails userDetails) throws LottabyteException {
        Integer versionId = referenceService.getLastVersionByPublishedId(publishedId, userDetails);

        if (sourceIds != null && !sourceIds.isEmpty()) {
            for (Artifact a : sourceIds) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setTargetId(newETLId);
                referenceEntity.setTargetType(ArtifactType.etl);
                referenceEntity.setSourceId(a.getId());
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setSourceType(a.getArtifactType());
                referenceEntity.setReferenceType(ReferenceType.SOURCE_TO_ETL);
                referenceEntity.setVersionId(versionId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.createReference(newReferenceEntity, userDetails);
            }
        }
    }

    public void updateReferenceForTarget(List<Artifact> targetIds, String newETLId, String publishedId,
                                         UserDetails userDetails) throws LottabyteException {
        if (targetIds != null && !targetIds.isEmpty()) {
            for (Artifact a : targetIds) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newETLId);
                referenceEntity.setTargetId(a.getId());
                referenceEntity.setTargetType(a.getArtifactType());
                referenceEntity.setPublishedId(publishedId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.patchReferenceBySourceIdAndTargetId(newReferenceEntity, userDetails);
            }
        }
    }

    public void createReferenceForTarget(List<Artifact> targetIds, String newETLId, String publishedId,
                                         UserDetails userDetails) throws LottabyteException {
        Integer versionId = referenceService.getLastVersionByPublishedId(publishedId, userDetails);

        if (targetIds != null && !targetIds.isEmpty()) {
            for (Artifact a : targetIds) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newETLId);
                referenceEntity.setSourceType(ArtifactType.etl);
                referenceEntity.setTargetId(a.getId());
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(a.getArtifactType());
                referenceEntity.setReferenceType(ReferenceType.ETL_TO_TARGET);
                referenceEntity.setVersionId(versionId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.createReference(newReferenceEntity, userDetails);
            }
        }
    }

    public void updateReferenceForBE(List<String> beIds, String newETLId, String publishedId,
                                         UserDetails userDetails) throws LottabyteException {
        if (beIds != null && !beIds.isEmpty()) {
            for (String id : beIds) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newETLId);
                referenceEntity.setTargetId(id);
                referenceEntity.setPublishedId(publishedId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.patchReferenceBySourceIdAndTargetId(newReferenceEntity, userDetails);
            }
        }
    }

    public void createReferenceForBE(List<String> beIds, String newETLId, String publishedId,
                                         UserDetails userDetails) throws LottabyteException {
        Integer versionId = referenceService.getLastVersionByPublishedId(publishedId, userDetails);

        if (beIds != null && !beIds.isEmpty()) {
            for (String id : beIds) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newETLId);
                referenceEntity.setSourceType(ArtifactType.etl);
                referenceEntity.setTargetId(id);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(ArtifactType.business_entity);
                referenceEntity.setReferenceType(ReferenceType.ETL_TO_BUSINESS_ENTITY);
                referenceEntity.setVersionId(versionId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.createReference(newReferenceEntity, userDetails);
            }
        }
    }

    public ETL wfPublish(String draftETLId, UserDetails userDetails) throws LottabyteException {
        ETL draft = getETLById(draftETLId, userDetails);
        String publishedId = ((WorkflowableMetadata) draft.getMetadata()).getPublishedId();
        if (draft == null)
            throw new LottabyteException(
                    Message.LBE03004,
                    userDetails.getLanguage(),
                    serviceArtifactType, draftETLId);
        ETL etl;

        if (publishedId == null) {
            publishedId = etlRepository.publishDraft(draftETLId, null, userDetails);

            if (draft.getEntity() != null) {
                updateReferenceForSource(draft.getEntity().getSourceIds(), draft.getId(), publishedId, userDetails);
                createReferenceForSource(draft.getEntity().getSourceIds(), publishedId, publishedId, userDetails);

                updateReferenceForTarget(draft.getEntity().getTargetIds(), draft.getId(), publishedId, userDetails);
                createReferenceForTarget(draft.getEntity().getTargetIds(), publishedId, publishedId, userDetails);

                updateReferenceForBE(draft.getEntity().getBusinessEntityIds(), draft.getId(), publishedId, userDetails);
                createReferenceForBE(draft.getEntity().getBusinessEntityIds(), publishedId, publishedId, userDetails);
            }

            tagService.mergeTags(draftETLId, serviceArtifactType, publishedId, serviceArtifactType, userDetails);

            etl = getETLById(publishedId, userDetails);

            elasticsearchService.insertElasticSearchEntity(
                    Collections.singletonList(getSearchableArtifact(etl, userDetails)), userDetails);
        } else {
            ETL currentPublished = getETLById(publishedId, userDetails);

            etlRepository.publishDraft(draftETLId, publishedId, userDetails);

            if (referenceService.getReferenceBySourceId(publishedId, userDetails) != null) {
                referenceService.deleteReferenceBySourceId(publishedId, userDetails);
                referenceService.deleteReferenceByTargetIdAndRefType(publishedId, ReferenceType.SOURCE_TO_ETL, userDetails);
            }

            if (draft.getEntity() != null) {
                updateReferenceForSource(draft.getEntity().getSourceIds(), draft.getId(), publishedId, userDetails);
                createReferenceForSource(draft.getEntity().getSourceIds(), publishedId, publishedId, userDetails);

                updateReferenceForTarget(draft.getEntity().getTargetIds(), draft.getId(), publishedId, userDetails);
                createReferenceForTarget(draft.getEntity().getTargetIds(), publishedId, publishedId, userDetails);

                updateReferenceForBE(draft.getEntity().getBusinessEntityIds(), draft.getId(), publishedId, userDetails);
                createReferenceForBE(draft.getEntity().getBusinessEntityIds(), publishedId, publishedId, userDetails);
            }

            tagService.mergeTags(draftETLId, serviceArtifactType, publishedId, serviceArtifactType, userDetails);

            etl = getETLById(publishedId, userDetails);

            elasticsearchService.updateElasticSearchEntity(
                    Collections.singletonList(getSearchableArtifact(etl, userDetails)),
                    userDetails);
        }

        return etl;
    }

    public void wfApproveRemoval(String draftETLId, UserDetails userDetails) throws LottabyteException {
        ETL etl = getETLById(draftETLId, userDetails);
        if (etl == null)
            throw new LottabyteException(
                    Message.LBE03004,
                    userDetails.getLanguage(),
                    serviceArtifactType, draftETLId);
        String publishedId = ((WorkflowableMetadata) etl.getMetadata()).getPublishedId();
        if (publishedId == null)
            throw new LottabyteException(
                    Message.LBE03006,
                    userDetails.getLanguage(),
                    serviceArtifactType, draftETLId);
        etlRepository.setStateById(etl.getId(), ArtifactState.DRAFT_HISTORY, userDetails);
        etlRepository.setStateById(publishedId, ArtifactState.REMOVED, userDetails);
        elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(publishedId), userDetails);
    }

    @Transactional
    public ETL createETL(UpdatableETLEntity newETLEntity, UserDetails userDetails)
            throws LottabyteException {
        if (newETLEntity.getName() == null || newETLEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE06402,
                    userDetails.getLanguage(), newETLEntity.getName());
        if (newETLEntity.getEtlTypeId() == null || newETLEntity.getEtlTypeId().isEmpty())
            throw new LottabyteException(Message.LBE06404,
                    userDetails.getLanguage(), newETLEntity.getName());
        if (etlRepository.getETLTypeById(newETLEntity.getEtlTypeId(), userDetails) == null)
            throw new LottabyteException(Message.LBE06404,
                    userDetails.getLanguage(), newETLEntity.getName());

        String workflowTaskId = null;
        ProcessInstance pi = null;
        newETLEntity.setId(UUID.randomUUID().toString());
        if (workflowService.isWorkflowEnabled(serviceArtifactType)
                && workflowService.getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

            pi = workflowService.startFlowableProcess(newETLEntity.getId(), serviceArtifactType,
                    ArtifactAction.CREATE,
                    userDetails);
            workflowTaskId = pi.getId();

        }
        String newETLId = etlRepository.createETL(newETLEntity, workflowTaskId, userDetails);

        createReferenceForSource(newETLEntity.getSourceIds(), newETLId, null, userDetails);
        createReferenceForTarget(newETLEntity.getTargetIds(), newETLId, null, userDetails);
        createReferenceForBE(newETLEntity.getBusinessEntityIds(), newETLId, null, userDetails);


        ETL etl = getETLById(newETLId, userDetails);

        // elasticsearchService.insertElasticSearchEntity(Collections.singletonList(indicator.getSearchableArtifact()),
        // userDetails);
        return etl;
    }

    public ETL patchETL(String etlId, UpdatableETLEntity etlEntity, boolean updateNulls,
                                    UserDetails userDetails) throws LottabyteException {
        ETL current = getETLById(etlId, userDetails);
        String draftId = null;

        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            draftId = etlRepository.getDraftId(etlId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE06403,
                        userDetails.getLanguage(),
                        draftId);
        }


        if (etlEntity.getName() != null && etlEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE06402,
                    userDetails.getLanguage(), etlEntity.getName());

        if (updateNulls && etlEntity.getBusinessEntityIds() == null)
            etlEntity.setBusinessEntityIds(new ArrayList<>());
        if (updateNulls && etlEntity.getSourceIds() == null)
            etlEntity.setSourceIds(new ArrayList<>());
        if (updateNulls && etlEntity.getTargetIds() == null)
            etlEntity.setTargetIds(new ArrayList<>());


        ProcessInstance pi = null;
        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            String workflowTaskId = null;
            draftId = UUID.randomUUID().toString();
            if (workflowService.isWorkflowEnabled(serviceArtifactType) && workflowService
                    .getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

                pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.UPDATE,
                        userDetails);
                workflowTaskId = pi.getId();

            }
            etlRepository.createDraftFromPublished(etlId, draftId, workflowTaskId, userDetails);

            if (etlEntity.getBusinessEntityIds() != null)
                createReferenceForBE(etlEntity.getBusinessEntityIds(), draftId, etlId, userDetails);
            else {
                if (current.getEntity().getBusinessEntityIds() != null && !current.getEntity().getBusinessEntityIds().isEmpty())
                    createReferenceForBE(current.getEntity().getBusinessEntityIds(), draftId, etlId, userDetails);
            }

            if (etlEntity.getSourceIds() != null)
                createReferenceForSource(etlEntity.getSourceIds(), draftId, etlId, userDetails);
            else {
                if (current.getEntity().getSourceIds() != null && !current.getEntity().getSourceIds().isEmpty())
                    createReferenceForSource(current.getEntity().getSourceIds(), draftId, etlId, userDetails);
            }

            if (etlEntity.getTargetIds() != null)
                createReferenceForTarget(etlEntity.getTargetIds(), draftId, etlId, userDetails);
            else {
                if (current.getEntity().getTargetIds() != null && !current.getEntity().getTargetIds().isEmpty())
                    createReferenceForTarget(current.getEntity().getTargetIds(), draftId, etlId, userDetails);
            }

            tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);
        } else {
            draftId = etlId;

            if (etlEntity.getBusinessEntityIds() != null) {
                if (current.getEntity().getBusinessEntityIds() != null && !current.getEntity().getBusinessEntityIds().isEmpty()) {
                    for (String id : current.getEntity().getBusinessEntityIds()) {
                        referenceService.deleteByReferenceSourceIdAndTargetId(draftId, id, userDetails);
                    }
                }
                createReferenceForBE(etlEntity.getBusinessEntityIds(), draftId, etlId, userDetails);
            }

            if (etlEntity.getSourceIds() != null) {
                if (current.getEntity().getSourceIds() != null && !current.getEntity().getSourceIds().isEmpty()) {
                    for (Artifact a : current.getEntity().getSourceIds()) {
                        referenceService.deleteByReferenceSourceIdAndTargetId(draftId, a.getId(), userDetails);
                    }
                }
                createReferenceForSource(etlEntity.getSourceIds(), draftId, etlId, userDetails);
            }

            if (etlEntity.getTargetIds() != null) {
                if (current.getEntity().getTargetIds() != null && !current.getEntity().getTargetIds().isEmpty()) {
                    for (Artifact a : current.getEntity().getTargetIds()) {
                        referenceService.deleteByReferenceSourceIdAndTargetId(draftId, a.getId(), userDetails);
                    }
                }
                createReferenceForTarget(etlEntity.getTargetIds(), draftId, etlId, userDetails);
            }
        }

        etlRepository.patchETL(draftId, etlEntity, updateNulls, userDetails);
        return getETLById(draftId, userDetails);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ETL deleteETL(String etlId, UserDetails userDetails) throws LottabyteException {
        ETL current = getETLById(etlId, userDetails);

        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            String draftId = etlRepository.getDraftId(etlId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE06403,
                        userDetails.getLanguage(),
                        draftId);

            ProcessInstance pi = null;
            draftId = null;
            String workflowTaskId = null;

            draftId = UUID.randomUUID().toString();
            pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.REMOVE, userDetails);
            workflowTaskId = pi.getId();

            etlRepository.createDraftFromPublished(current.getId(), draftId, workflowTaskId, userDetails);

            createReferenceForBE(current.getEntity().getBusinessEntityIds(), draftId, etlId, userDetails);
            createReferenceForSource(current.getEntity().getSourceIds(), draftId, etlId, userDetails);
            createReferenceForTarget(current.getEntity().getTargetIds(), draftId, etlId, userDetails);

            return getETLById(draftId, userDetails);
        } else {
            referenceService.deleteReferenceBySourceId(etlId, userDetails);
            tagService.deleteAllTagsByArtifactId(etlId, userDetails);
            etlRepository.deleteById(etlId, userDetails);
            return null;
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ETL archiveETLById(String etlId, UserDetails userDetails) throws LottabyteException {
        ETL current = getETLById(etlId, userDetails);

        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            String draftId = etlRepository.getDraftId(etlId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE06403,
                        userDetails.getLanguage(),
                        draftId);

            ProcessInstance pi = null;
            String workflowTaskId = null;

            draftId = UUID.randomUUID().toString();
            pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.ARCHIVE, userDetails);
            workflowTaskId = pi.getId();

            etlRepository.createDraftFromPublished(current.getId(), draftId, workflowTaskId, userDetails);

            createReferenceForBE(current.getEntity().getBusinessEntityIds(), draftId, etlId, userDetails);
            createReferenceForSource(current.getEntity().getSourceIds(), draftId, etlId, userDetails);
            createReferenceForTarget(current.getEntity().getTargetIds(), draftId, etlId, userDetails);

            return getETLById(draftId, userDetails);
        } else {
            String draftId = etlRepository.getDraftId(etlId, userDetails);
            throw new LottabyteException(
                    Message.LBE06403,
                    userDetails.getLanguage(),
                    draftId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ETL restoreETLById(String etlId, UserDetails userDetails) throws LottabyteException {
        ETL current = getETLById(etlId, userDetails);

        if (ArtifactState.ARCHIVED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            String draftId = etlRepository.getDraftId(etlId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE06403,
                        userDetails.getLanguage(),
                        draftId);

            ProcessInstance pi = null;
            String workflowTaskId = null;

            draftId = UUID.randomUUID().toString();
            pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.RESTORE, userDetails);
            workflowTaskId = pi.getId();

            etlRepository.createDraftFromPublished(current.getId(), draftId, workflowTaskId, userDetails);

            createReferenceForBE(current.getEntity().getBusinessEntityIds(), draftId, etlId, userDetails);
            createReferenceForSource(current.getEntity().getSourceIds(), draftId, etlId, userDetails);
            createReferenceForTarget(current.getEntity().getTargetIds(), draftId, etlId, userDetails);

            return getETLById(draftId, userDetails);
        } else {
            String draftId = etlRepository.getDraftId(etlId, userDetails);
            throw new LottabyteException(
                    Message.LBE06403,
                    userDetails.getLanguage(),
                    draftId);
        }
    }

    public SearchResponse<FlatETL> searchETL(SearchRequestWithJoin request, UserDetails userDetails)
            throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        SearchResponse<FlatETL> res = etlRepository.searchETL(request, searchableColumns,
                joinColumns, userDetails);
        res.getItems().stream().forEach(
                x -> x.setTags(tagService.getArtifactTags(x.getId(), userDetails)
                        .stream().map(y -> y.getName()).collect(Collectors.toList())));
        res.getItems().stream()
                .filter(x -> ArtifactState.DRAFT.equals(x.getState()) && x.getWorkflowTaskId() != null)
                .forEach(y -> {
                    WorkflowTask task = workflowService.getWorkflowTaskById(y.getWorkflowTaskId(), userDetails, false);
                    if (task != null)
                        y.setWorkflowState(task.getEntity().getWorkflowState());
                });
        res.getItems().forEach(d -> d.setIsInFav(userFavService.isInFav(d.getId(), userDetails)));
        return res;
    }

    public PaginatedArtifactList<ETL> getETLVersions(String etlId, Integer offset, Integer limit,
                                                                 UserDetails userDetails) throws LottabyteException {
        if (!etlRepository.existsById(etlId,
                new ArtifactState[] { ArtifactState.PUBLISHED, ArtifactState.REMOVED }, userDetails)) {
            PaginatedArtifactList<ETL> res = new PaginatedArtifactList<>();
            res.setCount(0);
            res.setOffset(offset);
            res.setLimit(limit);
            res.setResources(new ArrayList<>());
            return res;
        }
        PaginatedArtifactList<ETL> res = etlRepository.getVersionsById(etlId, offset, limit,
                "/v1/etl/", userDetails);
        for (ETL etl : res.getResources()) {
            fillETLVersionRelations(etl, userDetails);
        }
        return res;
    }

    public ETL getETLVersionById(String etlId, Integer versionId, UserDetails userDetails)
            throws LottabyteException {
        ETL etl = etlRepository.getVersionById(etlId, versionId, userDetails);
        if (etl == null)
            throw new LottabyteException(
                    Message.LBE06401,
                    userDetails.getLanguage(),
                    etl);
        fillETLVersionRelations(etl, userDetails);
        return etl;
    }

    public ETL restoreETLVersionById(String etlId, Integer versionId, UserDetails userDetails)
            throws LottabyteException {
        ETL etlVersion = getETLVersionById(etlId, versionId, userDetails);
        UpdatableETLEntity etlVersionEntity = new UpdatableETLEntity(etlVersion.getEntity());

        if (etlVersionEntity.getBusinessEntityIds() == null)
            etlVersionEntity.setBusinessEntityIds(new ArrayList<>());
        if (etlVersionEntity.getSourceIds() == null)
            etlVersionEntity.setSourceIds(new ArrayList<>());
        if (etlVersionEntity.getTargetIds() == null)
            etlVersionEntity.setTargetIds(new ArrayList<>());

        ETL etl = patchETL(etlId, etlVersionEntity, true, userDetails);

        WorkflowableMetadata versionMetadata = (WorkflowableMetadata)etlVersion.getMetadata();

        tagService.mergeTags(versionMetadata.getAncestorDraftId() == null ? etlVersion.getId() : versionMetadata.getAncestorDraftId(), serviceArtifactType, etl.getId(), serviceArtifactType, userDetails);

        return etl;
    }

    private void fillETLVersionRelations(ETL etl, UserDetails userDetails) {
        WorkflowableMetadata md = (WorkflowableMetadata) etl.getMetadata();
        if (md.getAncestorDraftId() != null) {
            etl.getMetadata().setTags(tagService.getArtifactTags(md.getAncestorDraftId(), userDetails));

            List<Reference> referenceForBE = referenceService.getAllReferenceBySourceIdAndRefType(
                    md.getAncestorDraftId(), ReferenceType.ETL_TO_BUSINESS_ENTITY, userDetails);
            etl.getEntity().setBusinessEntityIds(referenceForBE.stream().map(r -> r.getEntity().getTargetId()).collect(Collectors.toList()));

            List<Reference> referenceForSource = referenceService.getAllReferenceByTargetIdAndRefType(
                    md.getAncestorDraftId(), ReferenceType.SOURCE_TO_ETL, userDetails);
            etl.getEntity().setSourceIds(referenceForSource.stream().map(r -> new Artifact(r.getEntity().getSourceId(), r.getEntity().getSourceType())).collect(Collectors.toList()));

            List<Reference> referenceForTarget = referenceService.getAllReferenceBySourceIdAndRefType(
                    md.getAncestorDraftId(), ReferenceType.ETL_TO_TARGET, userDetails);
            etl.getEntity().setTargetIds(referenceForTarget.stream().map(r -> new Artifact(r.getEntity().getTargetId(), r.getEntity().getTargetType())).collect(Collectors.toList()));

        } else {

            List<Reference> referenceForBE = referenceService.getAllReferenceBySourceIdAndRefType(
                    etl.getId(), ReferenceType.ETL_TO_BUSINESS_ENTITY, userDetails);
            etl.getEntity().setBusinessEntityIds(referenceForBE.stream().map(r -> r.getEntity().getTargetId()).collect(Collectors.toList()));

            List<Reference> referenceForSource = referenceService.getAllReferenceByTargetIdAndRefType(
                    etl.getId(), ReferenceType.SOURCE_TO_ETL, userDetails);
            etl.getEntity().setSourceIds(referenceForSource.stream().map(r -> new Artifact(r.getEntity().getSourceId(), r.getEntity().getSourceType())).collect(Collectors.toList()));

            List<Reference> referenceForTarget = referenceService.getAllReferenceBySourceIdAndRefType(
                    etl.getId(), ReferenceType.ETL_TO_TARGET, userDetails);
            etl.getEntity().setTargetIds(referenceForTarget.stream().map(r -> new Artifact(r.getEntity().getTargetId(), r.getEntity().getTargetType())).collect(Collectors.toList()));
        }
    }

    public List<ETLType> getETLTypes(UserDetails userDetails) {
        return etlRepository.getETLTypes(userDetails);
    }

    public ETLType getETLTypeById(String id, UserDetails userDetails) {
        return etlRepository.getETLTypeById(id, userDetails);
    }

    @Override
    public String createDraft(String publishedId, WorkflowState workflowState, WorkflowType workflowType,
                              UserDetails userDetails) throws LottabyteException {
        ETL current = getById(publishedId, userDetails);

        ProcessInstance pi = null;
        String workflowTaskId = null;
        String draftId = UUID.randomUUID().toString();
        if (workflowService.isWorkflowEnabled(serviceArtifactType) && workflowService
                .getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

            pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.UPDATE,
                    userDetails);
            workflowTaskId = pi.getId();

        }
        etlRepository.createDraftFromPublished(publishedId, draftId, workflowTaskId, userDetails);

        if (current.getEntity().getBusinessEntityIds() != null && !current.getEntity().getBusinessEntityIds().isEmpty()) {
            for (String s : current.getEntity().getBusinessEntityIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(draftId);
                referenceEntity.setSourceType(ArtifactType.etl);
                referenceEntity.setTargetId(s);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(ArtifactType.business_entity);
                referenceEntity.setReferenceType(ReferenceType.ETL_TO_BUSINESS_ENTITY);
                referenceEntity.setVersionId(0);
                referenceService.createReference(new UpdatableReferenceEntity(referenceEntity), userDetails);
            }
        }

        if (current.getEntity().getSourceIds() != null && !current.getEntity().getSourceIds().isEmpty()) {
            for (Artifact a : current.getEntity().getSourceIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setTargetId(draftId);
                referenceEntity.setTargetType(ArtifactType.etl);
                referenceEntity.setSourceId(a.getId());
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setSourceType(a.getArtifactType());
                referenceEntity.setReferenceType(ReferenceType.SOURCE_TO_ETL);
                referenceEntity.setVersionId(0);
                referenceService.createReference(new UpdatableReferenceEntity(referenceEntity), userDetails);
            }
        }

        if (current.getEntity().getTargetIds() != null && !current.getEntity().getTargetIds().isEmpty()) {
            for (Artifact a : current.getEntity().getTargetIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(draftId);
                referenceEntity.setSourceType(ArtifactType.etl);
                referenceEntity.setTargetId(a.getId());
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(a.getArtifactType());
                referenceEntity.setReferenceType(ReferenceType.ETL_TO_TARGET);
                referenceEntity.setVersionId(0);
                referenceService.createReference(new UpdatableReferenceEntity(referenceEntity), userDetails);
            }
        }

        tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

        return draftId;
    }

    @Override
    public ETL getById(String id, UserDetails userDetails) throws LottabyteException {
        return getETLById(id, userDetails);
    }

    public SearchableETL getSearchableArtifact(ETL etl, UserDetails userDetails) {
        SearchableETL searchableETL = new SearchableETL();
        searchableETL.setId(etl.getMetadata().getId());
        searchableETL.setVersionId(etl.getMetadata().getVersionId());
        searchableETL.setName(etl.getMetadata().getName());
        searchableETL.setDescription(etl.getEntity().getDescription());
        searchableETL.setShortDescription(etl.getEntity().getShortDescription());

        searchableETL.setEtlTypeId(etl.getEntity().getEtlTypeId());
        searchableETL.setModifiedBy(etl.getMetadata().getModifiedBy());
        searchableETL.setModifiedAt(etl.getMetadata().getModifiedAt());
        searchableETL.setArtifactType(etl.getMetadata().getArtifactType());
        searchableETL.setArtifactState(((WorkflowableMetadata)etl.getMetadata()).getState().name());
        searchableETL.setEffectiveStartDate(etl.getMetadata().getEffectiveStartDate());
        searchableETL.setEffectiveEndDate(etl.getMetadata().getEffectiveEndDate());
        searchableETL.setTags(Helper.getEmptyListIfNull(etl.getMetadata().getTags()).stream()
                .map(Relation::getName).collect(Collectors.toList()));

        searchableETL.setCode(etl.getEntity().getCode());
        searchableETL.setAlgorithm(etl.getEntity().getAlgorithm());
        searchableETL.setSystemId(etl.getEntity().getSystemId());


        searchableETL.setRelatedArtifacts(new ArrayList<>());

        if (etl.getEntity().getSystemId() != null && !etl.getEntity().getSystemId().isEmpty()) {
            System s = systemRepository.getById(etl.getEntity().getSystemId(), userDetails);
            if (s != null)
                searchableETL.setSystemName(s.getName());
        }
        if (etl.getEntity().getEtlTypeId() != null
                && !etl.getEntity().getEtlTypeId().isEmpty()) {
            ETLType et = getETLTypeById(etl.getEntity().getEtlTypeId(), userDetails);
            if (et != null)
                searchableETL.setEtlTypeName(et.getName());
        }

        searchableETL.setDomains(new ArrayList<>());



        searchableETL.addRelatedArtifact("system", etl.getEntity().getSystemId());
        if (etl.getEntity().getSourceIds() != null) {
            for (Artifact a : etl.getEntity().getSourceIds()) {
                searchableETL.addRelatedArtifact(a.getArtifactType().getText(), a.getId());
            }
        }
        if (etl.getEntity().getTargetIds() != null) {
            for (Artifact a : etl.getEntity().getTargetIds()) {
                searchableETL.addRelatedArtifact(a.getArtifactType().getText(), a.getId());
            }
        }
        if (etl.getEntity().getBusinessEntityIds() != null) {
            for (String id : etl.getEntity().getBusinessEntityIds())
                searchableETL.addRelatedArtifact("business_entity", id);
        }

        return searchableETL;
    }
}
