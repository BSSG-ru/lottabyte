package ru.bssg.lottabyte.coreapi.service;

import org.flowable.engine.runtime.ProcessInstance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.businessEntity.BusinessEntity;
import ru.bssg.lottabyte.core.model.ca.CustomAttribute;
import ru.bssg.lottabyte.core.model.dataasset.DataAsset;
import ru.bssg.lottabyte.core.model.dataasset.FlatDataAsset;
import ru.bssg.lottabyte.core.model.dataasset.SearchableDataAsset;
import ru.bssg.lottabyte.core.model.dataasset.UpdatableDataAssetEntity;
import ru.bssg.lottabyte.core.model.dataentity.DataEntity;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.entitySample.EntitySample;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRule;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRuleEntity;
import ru.bssg.lottabyte.core.model.entitySample.UpdatableEntitySampleDQRule;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.model.workflow.WorkflowTask;
import ru.bssg.lottabyte.core.model.workflow.WorkflowType;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.*;
import ru.bssg.lottabyte.coreapi.util.Helper;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataAssetService extends WorkflowableService<DataAsset> {
        private final DataAssetRepository dataAssetRepository;
        private final BusinessEntityRepository businessEntityRepository;
        private final CustomAttributeDefinitionService customAttributeDefinitionService;
        private final EntitySampleRepository entitySampleRepository;
        private final SystemService systemService;
        private final DomainService domainService;
        private final EntityService entityService;
        private final TagService tagService;
        private final CommentService commentService;
        private final ElasticsearchService elasticsearchService;
        private final EntityQueryService entityQueryService;
        private final EntitySampleService entitySampleService;
        private final WorkflowService workflowService;
        private final ArtifactType serviceArtifactType = ArtifactType.data_asset;
        private final DQRuleService dqRuleService;

        private final SearchColumn[] searchableColumns = {
                        new SearchColumn("name", SearchColumn.ColumnType.Text),
                        new SearchColumn("description", SearchColumn.ColumnType.Text),
                        new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
                        new SearchColumn("system_id", SearchColumn.ColumnType.UUID),
                        new SearchColumn("domain_id", SearchColumn.ColumnType.UUID),
                        new SearchColumn("entity_id", SearchColumn.ColumnType.UUID),
                        new SearchColumn("system.name", SearchColumn.ColumnType.Text),
                        new SearchColumn("domain.name", SearchColumn.ColumnType.Text),
                        new SearchColumn("entity.name", SearchColumn.ColumnType.Text),
                        new SearchColumn("tags", SearchColumn.ColumnType.Text),
                        new SearchColumn("version_id", SearchColumn.ColumnType.Number),
                        new SearchColumn("workflow_state", SearchColumn.ColumnType.Text)
        };

        private final SearchColumnForJoin[] joinColumns = {
                        new SearchColumnForJoin("name", "domain", SearchColumn.ColumnType.Text, "domain_id", "id"),
                        new SearchColumnForJoin("name", "entity", SearchColumn.ColumnType.Text, "entity_id", "id"),
                        new SearchColumnForJoin("name", "system", SearchColumn.ColumnType.Text, "system_id", "id")
        };

        @Autowired
        public DataAssetService(DataAssetRepository dataAssetRepository,
                        CustomAttributeDefinitionService customAttributeDefinitionService,
                        SystemService systemService, DomainService domainService,
                        EntityService entityService, ElasticsearchService elasticsearchService,
                        TagService tagService, CommentService commentService,
                        EntityQueryService entityQueryService, EntitySampleService entitySampleService,
                        BusinessEntityRepository businessEntityRepository,
                        WorkflowService workflowService, DomainRepository domainRepository,
                        SystemRepository systemRepository,
                        EntityRepository entityRepository,
                        EntitySampleRepository entitySampleRepository,
                                DQRuleService dqRuleService) {
                super(dataAssetRepository, workflowService, tagService, ArtifactType.data_asset, elasticsearchService);
                this.dataAssetRepository = dataAssetRepository;
                this.customAttributeDefinitionService = customAttributeDefinitionService;
                this.systemService = systemService;
                this.domainService = domainService;
                this.entityService = entityService;
                this.elasticsearchService = elasticsearchService;
                this.tagService = tagService;
                this.commentService = commentService;
                this.entityQueryService = entityQueryService;
                this.entitySampleService = entitySampleService;
                this.workflowService = workflowService;
                this.businessEntityRepository = businessEntityRepository;
                this.entitySampleRepository = entitySampleRepository;
                this.dqRuleService = dqRuleService;
        }

        // WF interface

        @Override
        public String createDraft(String publishedId, WorkflowState workflowState, WorkflowType workflowType,
                        UserDetails userDetails) throws LottabyteException {

                DataAsset current = getDataAssetById(publishedId, userDetails);

                ProcessInstance pi = null;
                String workflowTaskId = null;
                String draftId = UUID.randomUUID().toString();
                if (workflowService.isWorkflowEnabled(serviceArtifactType) && workflowService
                        .getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

                        pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.UPDATE,
                                userDetails);
                        workflowTaskId = pi.getId();

                }
                dataAssetRepository.createDraftFromPublished(publishedId, draftId, workflowTaskId, userDetails);


                customAttributeDefinitionService.copyCustomAttributes(publishedId, draftId, serviceArtifactType,
                                userDetails);

                tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

                return draftId;
        }

        /*
         * public boolean existsInState(String artifactId, ArtifactState artifactState,
         * UserDetails userDetails) throws LottabyteException {
         * DataAsset dataAsset = getDataAssetById(artifactId, userDetails);
         * WorkflowableMetadata md = (WorkflowableMetadata)dataAsset.getMetadata();
         * if (!artifactState.equals(md.getState()))
         * return false;
         * return true;
         * }
         */

        /*
         * public String getDraftArtifactId(String publishedId, UserDetails userDetails)
         * {
         * //return dataAssetRepository.getDraftDataAssetId(publishedId, userDetails);
         * return dataAssetRepository.getDraftId(publishedId, userDetails);
         * }
         */

        /*
         * public String createDraft(String publishedId, WorkflowState workflowState,
         * UserDetails userDetails) throws LottabyteException {
         * String workflowTaskId = workflowService.getNewWorkflowTaskUUID().toString();
         * String draftId = dataAssetRepository.createDraftFromPublished(publishedId,
         * workflowTaskId, userDetails);
         * tagService.mergeTags(publishedId, serviceArtifactType, draftId,
         * serviceArtifactType, userDetails);
         * workflowService.postCreateDraft(workflowTaskId, WorkflowType.REMOVE, draftId,
         * serviceArtifactType, workflowState, userDetails);
         * return draftId;
         * 
         * //String draftId = createDraftDataAsset(current, workflowState, userDetails);
         * //return getDataAssetById(draftId, userDetails);
         * }
         */

        /*
         * public void wfCancel(String draftDataAssetId, UserDetails userDetails) throws
         * LottabyteException {
         * DataAsset current = dataAssetRepository.getById(draftDataAssetId,
         * userDetails);
         * if (current == null)
         * throw new LottabyteException(HttpStatus.NOT_FOUND, Message.LBE03004,
         * serviceArtifactType, draftDataAssetId);
         * if
         * (!ArtifactState.DRAFT.equals(((WorkflowableMetadata)current.getMetadata()).
         * getState()))
         * throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.LBE03003);
         * dataAssetRepository.setStateById(draftDataAssetId,
         * ArtifactState.DRAFT_HISTORY, userDetails);
         * }
         */

        /*
         * public void wfApproveRemoval(String draftDataAssetId, UserDetails
         * userDetails) throws LottabyteException {
         * DataAsset current = dataAssetRepository.getById(draftDataAssetId,
         * userDetails);
         * if (current == null)
         * throw new LottabyteException(HttpStatus.NOT_FOUND, Message.LBE03004,
         * serviceArtifactType, draftDataAssetId);
         * String publishedId =
         * ((WorkflowableMetadata)current.getMetadata()).getPublishedId();
         * if (publishedId == null)
         * throw new LottabyteException(HttpStatus.NOT_FOUND, Message.LBE03006,
         * serviceArtifactType, draftDataAssetId);
         * dataAssetRepository.setStateById(current.getId(),
         * ArtifactState.DRAFT_HISTORY, userDetails);
         * dataAssetRepository.setStateById(publishedId, ArtifactState.REMOVED,
         * userDetails);
         * elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(
         * publishedId), userDetails);
         * }
         */

        public DataAsset wfPublish(String draftDataAssetId, UserDetails userDetails) throws LottabyteException {
                DataAsset draft = getDataAssetById(draftDataAssetId, userDetails);
                draft.getEntity().setCustomAttributes(
                                customAttributeDefinitionService.getCustomAttributeByObjectId(draft.getId(),
                                                userDetails));
                String publishedId = ((WorkflowableMetadata) draft.getMetadata()).getPublishedId();
                if (draft == null)
                        throw new LottabyteException(
                                        Message.LBE03004,
                                                        userDetails.getLanguage(),
                                        serviceArtifactType, draftDataAssetId);
                // TODO: Validate Data Asset name unuqieness
                // if (entityRepository.dataEntityNameExists(draft.getEntity().getName(),
                // draft.getEntity().getEntityFolderId(), publishedId, userDetails))
                // throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.LBE00327,
                // draft.getEntity().getName());
                DataAsset dataAsset;

                if (publishedId == null) {
                        publishedId = dataAssetRepository.publishDraft(draftDataAssetId, null, userDetails);

                        if (draft.getEntity().getDqRules() != null && !draft.getEntity().getDqRules().isEmpty())
                                mergeDQRules(draft.getId(), publishedId, draft.getEntity().getDqRules(), null, userDetails);

                        tagService.mergeTags(draftDataAssetId, serviceArtifactType, publishedId, serviceArtifactType,
                                        userDetails);
                        customAttributeDefinitionService.patchCustomAttributes(draft.getEntity().getCustomAttributes(),
                                        publishedId, serviceArtifactType.getText(), userDetails, false);
                        dataAsset = getDataAssetById(publishedId, userDetails);
                        elasticsearchService.insertElasticSearchEntity(
                                        Collections.singletonList(getSearchableArtifact(dataAsset, userDetails)),
                                        userDetails);
                } else {
                        DataAsset currentPublished = getDataAssetById(publishedId, userDetails);

                        if (currentPublished.getEntity().getDqRules() != null) {
                                for (EntitySampleDQRule rule : currentPublished.getEntity().getDqRules()) {
                                        boolean isDeleting = draft.getEntity().getDqRules() == null || draft.getEntity().getDqRules().stream().noneMatch(x -> x.getEntity().getAncestorId().equals(rule.getId()));
                                        if (isDeleting && dqRuleService.existsInLog(rule.getId(), userDetails)) {
                                                throw new LottabyteException(Message.LBE03206, userDetails.getLanguage());
                                        }
                                }
                        }

                        dataAssetRepository.publishDraft(draftDataAssetId, publishedId, userDetails);

                        mergeDQRules(draft.getId(), publishedId, draft.getEntity().getDqRules(), currentPublished.getEntity().getDqRules(), userDetails);

                        tagService.mergeTags(draftDataAssetId, serviceArtifactType, publishedId, serviceArtifactType,
                                        userDetails);
                        customAttributeDefinitionService.patchCustomAttributes(draft.getEntity().getCustomAttributes(),
                                        publishedId, serviceArtifactType.getText(), userDetails, false);
                        dataAsset = getDataAssetById(publishedId, userDetails);
                        elasticsearchService.updateElasticSearchEntity(
                                        Collections.singletonList(getSearchableArtifact(dataAsset, userDetails)),
                                        userDetails);
                }
                return dataAsset;
                /*
                 * } else {
                 * dataAssetRepository.publishDataAssetDraft(draftDataAssetId, publishedId,
                 * userDetails);
                 * tagService.mergeTags(draftDataAssetId, serviceArtifactType, publishedId,
                 * serviceArtifactType, userDetails);
                 * DataAsset da = getDataAssetById(publishedId, userDetails);
                 * elasticsearchService.insertElasticSearchEntity(Collections.singletonList(da.
                 * getSearchableArtifact()), userDetails);
                 * return da;
                 * }
                 */
        }

        // Data Asset

        public Boolean allDataAssetsExist(List<String> systemIds, UserDetails userDetails) {
                return dataAssetRepository.allDataAssetsExist(systemIds, userDetails);
        }

        private String createDraftDataAsset1(String id, WorkflowState workflowState, UserDetails userDetails)
                        throws LottabyteException {
                String workflowTaskId = workflowService.getNewWorkflowTaskUUID().toString();
                String draftId = dataAssetRepository.createDraftFromPublished(id, null, workflowTaskId, userDetails);
                tagService.mergeTags(id, serviceArtifactType, draftId, serviceArtifactType, userDetails);
                workflowService.postCreateDraft(workflowTaskId, WorkflowType.REMOVE, draftId, serviceArtifactType,
                                workflowState, userDetails);
                return draftId;
        }

        public DataAsset getById(String id, UserDetails userDetails) throws LottabyteException {
                return getDataAssetById(id, userDetails);
        }

        public DataAsset getDataAssetById(String id, UserDetails userDetails) throws LottabyteException {
                DataAsset dataAsset = dataAssetRepository.getById(id, userDetails);
                if (dataAsset == null)
                        throw new LottabyteException(Message.LBE00503,
                                        userDetails.getLanguage(), id);
                List<EntitySampleDQRule> dqRules = entitySampleRepository.getSampleDQRulesByAsset(id, userDetails);
                if (((WorkflowableMetadata)dataAsset.getMetadata()).getState().equals(ArtifactState.PUBLISHED)) {
                        for (EntitySampleDQRule r : dqRules) {
                                r.getEntity().setAncestorId(r.getId());
                        }
                }
                dataAsset.getEntity().setDqRules(dqRules);

                dataAsset.getEntity().setCustomAttributes(
                                customAttributeDefinitionService.getCustomAttributeByObjectId(dataAsset.getId(),
                                                userDetails));
                dataAsset.getMetadata().setTags(tagService.getArtifactTags(id, userDetails));

                dataAsset.getEntity().setHasStatistics(
                                dataAsset.getEntity().getDataSize() != null
                                                && dataAsset.getEntity().getRowsCount() != null);
                if (dataAsset.getEntity().getEntityId() != null && dataAsset.getEntity().getSystemId() != null) {
                        EntitySample mainEntitySample = entitySampleService.getMainEntitySampleByEntityIdAndSystemId(
                                        dataAsset.getEntity().getEntityId(), dataAsset.getEntity().getSystemId(),
                                        userDetails);
                        dataAsset.getEntity().setHasSample(mainEntitySample != null);
                        if (mainEntitySample != null) {
                                dataAsset.getEntity().setHasQuery(entityQueryService
                                                .getEntityQueryById(mainEntitySample.getEntity().getEntityQueryId(),
                                                                userDetails) != null);
                        } else {
                                dataAsset.getEntity()
                                                .setHasQuery(entityQueryService.getEntityQueryByEntityIdAndSystemId(
                                                                dataAsset.getEntity().getEntityId(),
                                                                dataAsset.getEntity().getSystemId(),
                                                                userDetails) != null);
                        }
                }
                WorkflowableMetadata md = (WorkflowableMetadata) dataAsset.getMetadata();
                if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
                        md.setDraftId(dataAssetRepository.getDraftId(md.getId(), userDetails));
                return dataAsset;
        }

        public DataAsset getDataAssetByIdAndStation(String id, ArtifactState artifactState, UserDetails userDetails)
                        throws LottabyteException {
                DataAsset dataAsset = dataAssetRepository.getByIdAndStation(id, artifactState, userDetails);
                if (dataAsset == null)
                        throw new LottabyteException(Message.LBE00503,
                                        userDetails.getLanguage(), id);
                dataAsset.getEntity().setCustomAttributes(
                                customAttributeDefinitionService.getCustomAttributeByObjectId(dataAsset.getId(),
                                                userDetails));
                dataAsset.getMetadata().setTags(tagService.getArtifactTags(id, userDetails));

                dataAsset.getEntity().setHasStatistics(
                                dataAsset.getEntity().getDataSize() != null
                                                && dataAsset.getEntity().getRowsCount() != null);
                if (dataAsset.getEntity().getEntityId() != null && dataAsset.getEntity().getSystemId() != null) {
                        EntitySample mainEntitySample = entitySampleService.getMainEntitySampleByEntityIdAndSystemId(
                                        dataAsset.getEntity().getEntityId(), dataAsset.getEntity().getSystemId(),
                                        userDetails);
                        dataAsset.getEntity().setHasSample(mainEntitySample != null);
                        if (mainEntitySample != null) {
                                dataAsset.getEntity().setHasQuery(entityQueryService
                                                .getEntityQueryById(mainEntitySample.getEntity().getEntityQueryId(),
                                                                userDetails) != null);
                        } else {
                                dataAsset.getEntity()
                                                .setHasQuery(entityQueryService.getEntityQueryByEntityIdAndSystemId(
                                                                dataAsset.getEntity().getEntityId(),
                                                                dataAsset.getEntity().getSystemId(),
                                                                userDetails) != null);
                        }
                }
                WorkflowableMetadata md = (WorkflowableMetadata) dataAsset.getMetadata();
                if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
                        md.setDraftId(dataAssetRepository.getDraftId(md.getId(), userDetails));
                return dataAsset;
        }



        public PaginatedArtifactList<DataAsset> getAllDataAssetPaginated(Integer offset, Integer limit,
                        String artifactState, UserDetails userDetails) throws LottabyteException {
                if (!EnumUtils.isValidEnum(ArtifactState.class, artifactState))
                        throw new LottabyteException(
                                        Message.LBE00067,
                                                        userDetails.getLanguage(),
                                        artifactState);
                PaginatedArtifactList<DataAsset> dataAssetPaginatedArtifactList = dataAssetRepository.getAllPaginated(
                                offset,
                                limit, "/v1/data_assets",
                                ArtifactState.valueOf(artifactState), userDetails);
                for (DataAsset dataAsset : dataAssetPaginatedArtifactList.getResources()) {
                        dataAsset.getEntity().setCustomAttributes(
                                        customAttributeDefinitionService.getCustomAttributeByObjectId(dataAsset.getId(),
                                                        userDetails));
                        dataAsset.getMetadata().setTags(tagService.getArtifactTags(dataAsset.getId(), userDetails));

                        dataAsset.getEntity().setHasStatistics(
                                        dataAsset.getEntity().getDataSize() != null
                                                        && dataAsset.getEntity().getRowsCount() != null);
                        if (dataAsset.getEntity().getEntityId() != null
                                        && dataAsset.getEntity().getSystemId() != null) {
                                EntitySample mainEntitySample = entitySampleService
                                                .getMainEntitySampleByEntityIdAndSystemId(
                                                                dataAsset.getEntity().getEntityId(),
                                                                dataAsset.getEntity().getSystemId(), userDetails);
                                dataAsset.getEntity().setHasSample(mainEntitySample != null);
                                if (mainEntitySample != null) {
                                        dataAsset.getEntity().setHasQuery(entityQueryService
                                                        .getEntityQueryById(
                                                                        mainEntitySample.getEntity().getEntityQueryId(),
                                                                        userDetails) != null);
                                } else {
                                        dataAsset.getEntity()
                                                        .setHasQuery(entityQueryService
                                                                        .getEntityQueryByEntityIdAndSystemId(
                                                                                        dataAsset.getEntity()
                                                                                                        .getEntityId(),
                                                                                        dataAsset.getEntity()
                                                                                                        .getSystemId(),
                                                                                        userDetails) != null);
                                }
                        }
                }
                return dataAssetPaginatedArtifactList;
        }

        public List<DataAsset> getDataAssetByEntityId(String entityId, UserDetails userDetails) {
                return dataAssetRepository.getBy("entity_id", UUID.fromString(entityId), userDetails);
        }

        public PaginatedArtifactList<DataAsset> getDataAssetVersions(String assetId, Integer offset, Integer limit,
                        UserDetails userDetails) throws LottabyteException {
                if (!dataAssetRepository.existsById(assetId,
                                new ArtifactState[] { ArtifactState.PUBLISHED, ArtifactState.REMOVED }, userDetails)) {
                        // throw new LottabyteException(HttpStatus.NOT_FOUND,
                        // Message.format(Message.LBE00503.getText(userDetails.getLanguage().name()),
                        // userDetails.getLanguage().name(), assetId));
                        PaginatedArtifactList<DataAsset> res = new PaginatedArtifactList<>();
                        res.setCount(0);
                        res.setOffset(offset);
                        res.setLimit(limit);
                        res.setResources(new ArrayList<>());
                        return res;
                }
                PaginatedArtifactList<DataAsset> res = dataAssetRepository.getVersionsById(assetId, offset, limit,
                                "/v1/data_assets", userDetails);
                // TODO: SetHasQuery!
                for (DataAsset dataAsset : res.getResources()) {
                        fillDataAssetVersionRelations(dataAsset, userDetails);

                        dataAsset.getEntity().setHasStatistics(
                                        dataAsset.getEntity().getDataSize() != null
                                                        && dataAsset.getEntity().getRowsCount() != null);
                        if (dataAsset.getEntity().getEntityId() != null
                                        && dataAsset.getEntity().getSystemId() != null) {
                                EntitySample mainEntitySample = entitySampleService
                                                .getMainEntitySampleByEntityIdAndSystemId(
                                                                dataAsset.getEntity().getEntityId(),
                                                                dataAsset.getEntity().getSystemId(), userDetails);
                                dataAsset.getEntity().setHasSample(mainEntitySample != null);
                                if (mainEntitySample != null) {
                                        dataAsset.getEntity().setHasQuery(entityQueryService
                                                        .getEntityQueryById(
                                                                        mainEntitySample.getEntity().getEntityQueryId(),
                                                                        userDetails) != null);
                                } else {
                                        dataAsset.getEntity()
                                                        .setHasQuery(entityQueryService
                                                                        .getEntityQueryByEntityIdAndSystemId(
                                                                                        dataAsset.getEntity()
                                                                                                        .getEntityId(),
                                                                                        dataAsset.getEntity()
                                                                                                        .getSystemId(),
                                                                                        userDetails) != null);
                                }
                        }
                }
                return res;
        }

        private void validateSystem(UpdatableDataAssetEntity da, String currentSystemId, UserDetails userDetails)
                        throws LottabyteException {
                systemService.getSystemByIdAndState(da.getSystemId(), ArtifactState.PUBLISHED, userDetails);
                if (userDetails.getStewardId() != null) {
                        if (!systemService.hasAccessToSystem(da.getSystemId(), userDetails))
                                throw new LottabyteException(

                                                                Message.LBE00921,
                                                                                userDetails.getLanguage(),
                                                da.getSystemId());
                        if (currentSystemId != null && !currentSystemId.equals(da.getSystemId()) &&
                                        !systemService.hasAccessToSystem(currentSystemId, userDetails))
                                throw new LottabyteException(Message.LBE00923, userDetails.getLanguage(), currentSystemId);
                }
        }

        private void validateDomain(UpdatableDataAssetEntity da, String currentDomainId, UserDetails userDetails)
                        throws LottabyteException {
                domainService.getDomainByIdAndState(da.getDomainId(), userDetails);
                if (userDetails.getStewardId() != null) {
                        if (!domainService.hasAccessToDomain(da.getDomainId(), userDetails))
                                throw new LottabyteException(
                                                                Message.LBE00116,
                                                                                userDetails.getLanguage(),
                                                da.getDomainId());
                        if (currentDomainId != null && !currentDomainId.equals(da.getDomainId()) &&
                                        !domainService.hasAccessToDomain(currentDomainId, userDetails))
                                throw new LottabyteException(
                                                                Message.LBE00117,
                                                                                userDetails.getLanguage(),
                                                currentDomainId);
                }
        }

        private void validateEntity(UpdatableDataAssetEntity da, String currentEntityId, UserDetails userDetails)
                        throws LottabyteException {
                entityService.getDataEntityByIdAndState(da.getEntityId(), ArtifactState.PUBLISHED, userDetails);
                if (userDetails.getStewardId() != null) {
                        if (!entityService.hasAccessToEntity(da.getEntityId(), userDetails))
                                throw new LottabyteException(
                                                                Message.LBE00324,
                                                                                userDetails.getLanguage(),
                                                da.getEntityId());
                        if (currentEntityId != null && !currentEntityId.equals(da.getEntityId()) &&
                                        !entityService.hasAccessToEntity(currentEntityId, userDetails))
                                throw new LottabyteException(
                                                                Message.LBE00325,
                                                                                userDetails.getLanguage(),
                                                currentEntityId);
                }
        }

        @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
        public DataAsset createDataAsset(UpdatableDataAssetEntity newDataAssetEntity, UserDetails userDetails)
                        throws LottabyteException {
                if (newDataAssetEntity.getName() == null || newDataAssetEntity.getName().isEmpty())
                        throw new LottabyteException(
                                        Message.LBE00501,
                                        userDetails.getLanguage());
                if (newDataAssetEntity.getSystemId() != null)
                        validateSystem(newDataAssetEntity, null, userDetails);
                if (newDataAssetEntity.getDomainId() != null)
                        validateDomain(newDataAssetEntity, null, userDetails);
                if (newDataAssetEntity.getEntityId() != null)
                        validateEntity(newDataAssetEntity, null, userDetails);
                /*
                 * if (dataAssetRepository.existsDataAssetByName(newDataAssetEntity.getName(),
                 * null, userDetails)) {
                 * throw new LottabyteException(HttpStatus.BAD_REQUEST,
                 * Message.format(Message.LBE00502, newDataAssetEntity.getName()));
                 * }
                 */
                if (newDataAssetEntity.getCustomAttributes() != null
                                && !newDataAssetEntity.getCustomAttributes().isEmpty()) {
                        Set<String> caDefIds = new HashSet<>();
                        for (CustomAttribute ca : newDataAssetEntity.getCustomAttributes()) {
                                customAttributeDefinitionService.validateCustomAttribute(ca, caDefIds, userDetails);
                        }
                }

                newDataAssetEntity.setId(UUID.randomUUID().toString());
                String workflowTaskId = null;
                ProcessInstance pi = null;
                if (workflowService.isWorkflowEnabled(serviceArtifactType)
                                && workflowService.getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH,
                                                userDetails) != null) {

                        pi = workflowService.startFlowableProcess(newDataAssetEntity.getId(), serviceArtifactType,
                                        ArtifactAction.CREATE,
                                        userDetails);
                        workflowTaskId = pi.getId();

                }

                String newDataAssetId = dataAssetRepository.createDataAsset(newDataAssetEntity, workflowTaskId,
                                userDetails);
                customAttributeDefinitionService.createCustomAttributes(newDataAssetEntity.getCustomAttributes(),
                                newDataAssetId, ArtifactType.data_asset.getText(), userDetails, false);
                DataAsset result = getDataAssetById(newDataAssetId, userDetails);

                // elasticsearchService.insertElasticSearchEntity(Collections.singletonList(result.getSearchableArtifact()),
                // userDetails);
                return result;
        }

        @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
        public DataAsset patchDataAsset(String dataAssetId, UpdatableDataAssetEntity dataAssetEntity,
                        UserDetails userDetails) throws LottabyteException {
                DataAsset current = getDataAssetById(dataAssetId, userDetails);
                if (current == null)
                        throw new LottabyteException(Message.LBE00503,
                                                        userDetails.getLanguage(), dataAssetId);
                String draftId = null;
                if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
                        // draftId = dataAssetRepository.getDraftDataAssetId(dataAssetId, userDetails);
                        draftId = dataAssetRepository.getDraftId(dataAssetId, userDetails);
                        if (draftId != null && !draftId.isEmpty())
                                throw new LottabyteException(Message.LBE00505, userDetails.getLanguage(), draftId);
                }

                if (userDetails.getStewardId() != null
                                && !dataAssetRepository.hasAccessToDataAsset(dataAssetId, userDetails))
                        throw new LottabyteException(Message.LBE00504,
                                                        userDetails.getLanguage(), dataAssetId);
                if (dataAssetEntity.getName() != null && dataAssetEntity.getName().isEmpty())
                        throw new LottabyteException(Message.LBE00501,
                                        userDetails.getLanguage());
                if (dataAssetEntity.getSystemId() != null && !dataAssetEntity.getSystemId().isEmpty())
                        validateSystem(dataAssetEntity, current.getEntity().getSystemId(), userDetails);
                if (dataAssetEntity.getDomainId() != null && !dataAssetEntity.getDomainId().isEmpty())
                        validateDomain(dataAssetEntity, current.getEntity().getDomainId(), userDetails);
                if (dataAssetEntity.getEntityId() != null && !dataAssetEntity.getEntityId().isEmpty())
                        validateEntity(dataAssetEntity, current.getEntity().getEntityId(), userDetails);
                if (dataAssetEntity.getCustomAttributes() != null && !dataAssetEntity.getCustomAttributes().isEmpty()) {
                        Set<String> caDefIds = new HashSet<>();
                        for (CustomAttribute ca : dataAssetEntity.getCustomAttributes()) {
                                customAttributeDefinitionService.validateCustomAttribute(ca, caDefIds, userDetails);
                        }
                }

                if (current.getEntity().getDqRules() != null) {
                        for (EntitySampleDQRule rule : current.getEntity().getDqRules()) {
                                boolean isDeleting = dataAssetEntity.getDqRules() != null && dataAssetEntity.getDqRules().stream().noneMatch(x -> x.getId().equals(rule.getId()));
                                if (isDeleting && dqRuleService.existsInLog(rule.getId(), userDetails)) {
                                        throw new LottabyteException(Message.LBE03206, userDetails.getLanguage());
                                }
                        }
                }

                ProcessInstance pi = null;
                if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {

                        String workflowTaskId = null;
                        draftId = UUID.randomUUID().toString();
                        if (workflowService.isWorkflowEnabled(serviceArtifactType) && workflowService
                                        .getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH,
                                                        userDetails) != null) {

                                pi = workflowService.startFlowableProcess(draftId, serviceArtifactType,
                                                ArtifactAction.UPDATE,
                                                userDetails);
                                workflowTaskId = pi.getId();

                        }
                        draftId = dataAssetRepository.createDraftFromPublished(dataAssetId, draftId, workflowTaskId,
                                        userDetails);

                        if (dataAssetEntity.getDqRules() != null) {
                                createDQRulesLinks(dataAssetEntity.getDqRules(), draftId, dataAssetId, userDetails, false);
                        } else if (current.getEntity().getDqRules() != null && !current.getEntity().getDqRules().isEmpty()) {
                                createDQRulesLinks(current.getEntity().getDqRules(), draftId, dataAssetId, userDetails, true);
                        }

                        customAttributeDefinitionService.copyCustomAttributes(dataAssetId, draftId, serviceArtifactType,
                                        userDetails);
                        tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType,
                                        userDetails);

                } else {
                        draftId = dataAssetId;
                        updateDQRulesLinks(dataAssetId, ((WorkflowableMetadata) current.getMetadata()).getPublishedId(), dataAssetEntity.getDqRules(), current.getEntity().getDqRules(), userDetails);
                }

                dataAssetRepository.patchDataAsset(draftId, dataAssetEntity, userDetails);
                customAttributeDefinitionService.patchCustomAttributes(dataAssetEntity.getCustomAttributes(),
                                draftId, ArtifactType.data_asset.getText(), userDetails, false);

                DataAsset result = getDataAssetById(draftId, userDetails);
                // elasticsearchService.updateElasticSearchEntity(Collections.singletonList(result.getSearchableArtifact()),
                // userDetails);
                return result;
        }

        private void updateDQRules(String assetId, List<EntitySampleDQRule> ids, List<EntitySampleDQRule> currentIds,
                        UserDetails userDetails) {
                if (currentIds != null && ids != null) {
                        ids.stream().filter(x -> EntitySampleService.containsDQRule(currentIds, x)).forEach(y -> dataAssetRepository.updateDQRule(assetId, y.getEntity().getDqRuleId(), y, userDetails));
                        ids.stream().filter(x -> !EntitySampleService.containsDQRule(currentIds, x))
                                        .collect(Collectors.toList())
                                        .forEach(y -> dataAssetRepository.addDQRule(assetId, y, userDetails));
                        currentIds.stream().filter(x -> !EntitySampleService.containsDQRule(ids, x))
                                        .collect(Collectors.toList())
                                        .forEach(y -> dataAssetRepository.removeDQRule(y.getId(), userDetails));
                }
        }

        @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
        public DataAsset deleteDataAsset(String dataAssetId, UserDetails userDetails) throws LottabyteException {
                DataAsset current = getDataAssetById(dataAssetId, userDetails);
                if (userDetails.getStewardId() != null
                                && !dataAssetRepository.hasAccessToDataAsset(dataAssetId, userDetails))
                        throw new LottabyteException(Message.LBE00504,
                                                        userDetails.getLanguage(), dataAssetId);

                if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
                        String draftId = dataAssetRepository.getDraftId(dataAssetId, userDetails);
                        if (draftId != null && !draftId.isEmpty())
                                throw new LottabyteException(
                                                                Message.LBE00505,
                                                                userDetails.getLanguage(),
                                                draftId);
                        ProcessInstance pi = null;
                        draftId = null;
                        String workflowTaskId = null;

                        draftId = UUID.randomUUID().toString();
                        pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.REMOVE, userDetails);
                        workflowTaskId = pi.getId();

                        dataAssetRepository.createDraftFromPublished(current.getId(), draftId, workflowTaskId, userDetails);

                        customAttributeDefinitionService.copyCustomAttributes(dataAssetId, draftId, serviceArtifactType,
                                        userDetails);
                        return getDataAssetById(draftId, userDetails);
                } else {
                        customAttributeDefinitionService.deleteAllCustomAttributesByArtifactId(dataAssetId,
                                        userDetails);
                        tagService.deleteAllTagsByArtifactId(dataAssetId, userDetails);
                        dataAssetRepository.deleteById(dataAssetId, userDetails);
                        return null;
                }

                /*
                 * customAttributeDefinitionService.deleteAllCustomAttributesByArtifactId(
                 * dataAssetId, userDetails);
                 * tagService.deleteAllTagsByArtifactId(dataAssetId, userDetails);
                 * commentService.deleteAllCommentsByArtifactId(dataAssetId, userDetails);
                 * dataAssetRepository.deleteDataAsset(dataAssetId, userDetails);
                 * ArchiveResponse archiveResponse = new ArchiveResponse();
                 * archiveResponse.setArchivedGuids(Collections.singletonList(dataAssetId));
                 * elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(
                 * dataAssetId), userDetails);
                 * return archiveResponse;
                 */
        }

        public SearchResponse<FlatDataAsset> searchDataAssets(SearchRequestWithJoin request, UserDetails userDetails)
                        throws LottabyteException {
                ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
                SearchResponse<FlatDataAsset> res = dataAssetRepository.searchDataAssets(request, searchableColumns,
                                joinColumns, userDetails);
                res.getItems().stream().forEach(
                                x -> x.setTags(tagService.getArtifactTags(x.getId(), userDetails)
                                                .stream().map(y -> y.getName()).collect(Collectors.toList())));
                res.getItems().stream()
                                .filter(x -> ArtifactState.DRAFT.equals(x.getState()) && x.getWorkflowTaskId() != null)
                                .forEach(y -> {
                                        WorkflowTask task = workflowService.getWorkflowTaskById(y.getWorkflowTaskId(),
                                                        userDetails, false);
                                        if (task != null)
                                                y.setWorkflowState(task.getEntity().getWorkflowState());
                                });
                return res;
        }

        public DataAsset getDataAssetVersionById(String dataAssetId, Integer versionId, UserDetails userDetails)
                        throws LottabyteException {

                DataAsset dataAsset = dataAssetRepository.getVersionById(dataAssetId, versionId, userDetails);
                if (dataAsset == null)
                        throw new LottabyteException(Message.LBE00506,
                                                        userDetails.getLanguage(), dataAssetId, versionId);

                fillDataAssetVersionRelations(dataAsset, userDetails);
                return dataAsset;
        }

        private void fillDataAssetVersionRelations(DataAsset dataAsset, UserDetails userDetails)
                        throws LottabyteException {
                WorkflowableMetadata md = (WorkflowableMetadata) dataAsset.getMetadata();

                String id = (md.getAncestorDraftId() == null ? dataAsset.getId() : md.getAncestorDraftId());

                dataAsset.getMetadata()
                                .setTags(tagService.getArtifactTags(id, userDetails));
                dataAsset.getEntity().setCustomAttributes(
                                customAttributeDefinitionService.getCustomAttributeByObjectId(
                                                id,
                                                userDetails));

                List<EntitySampleDQRule> dqRules = entitySampleRepository.getSampleDQRulesByProduct(id, userDetails);
                dataAsset.getEntity().setDqRules(dqRules);
        }

        public SearchResponse<FlatDataAsset> searchDataAssetsByBE(SearchRequestWithJoin request, String beId,
                        UserDetails userDetails) throws LottabyteException {
                ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
                if (beId == null || beId.isEmpty() || !beId
                                .matches("[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}")) {
                        throw new LottabyteException(Message.LBE02501,
                                                        userDetails.getLanguage(), beId);
                }
                BusinessEntity be = businessEntityRepository.getById(beId, userDetails);
                if (request.getLimitSteward() != null && request.getLimitSteward() && userDetails.getStewardId() != null
                                &&
                                !domainService.hasAccessToDomain(be.getEntity().getDomainId(), userDetails))
                        return new SearchResponse<>(0, request.getLimit(), request.getOffset(), new ArrayList<>());

                SearchResponse<FlatDataAsset> res = dataAssetRepository.searchDataAssetsByBE(request, beId,
                                searchableColumns,
                                joinColumns, userDetails);
                res.getItems().stream().forEach(
                                x -> x.setTags(tagService.getArtifactTags(x.getId(), userDetails)
                                                .stream().map(y -> y.getName()).collect(Collectors.toList())));
                res.getItems().stream()
                                .filter(x -> ArtifactState.DRAFT.equals(x.getState()) && x.getWorkflowTaskId() != null)
                                .forEach(y -> {
                                        WorkflowTask task = workflowService.getWorkflowTaskById(y.getWorkflowTaskId(),
                                                        userDetails, false);
                                        if (task != null)
                                                y.setWorkflowState(task.getEntity().getWorkflowState());
                                });
                return res;
        }

        public SearchableDataAsset getSearchableArtifact(DataAsset dataAsset, UserDetails userDetails)
                        throws LottabyteException {
                SearchableDataAsset sa = SearchableDataAsset.builder()
                        .id(dataAsset.getMetadata().getId())
                        .versionId(dataAsset.getMetadata().getVersionId())
                        .name(dataAsset.getMetadata().getName())
                        .description(dataAsset.getEntity().getDescription())
                        .modifiedBy(dataAsset.getMetadata().getModifiedBy())
                        .modifiedAt(dataAsset.getMetadata().getModifiedAt())
                        .artifactType(dataAsset.getMetadata().getArtifactType())
                        .effectiveStartDate(dataAsset.getMetadata().getEffectiveStartDate())
                        .effectiveEndDate(dataAsset.getMetadata().getEffectiveEndDate())
                        .tags(Helper.getEmptyListIfNull(dataAsset.getMetadata().getTags()).stream()
                                        .map(x -> x.getName()).collect(Collectors.toList()))

                        .systemId(dataAsset.getEntity().getSystemId())
                        .domainId(dataAsset.getEntity().getDomainId())
                        .entityId(dataAsset.getEntity().getEntityId())
                        .roles(dataAsset.getEntity().getRoles())
                        .dataSize(dataAsset.getEntity().getDataSize())
                        .hasQuery(dataAsset.getEntity().getHasQuery())
                        .hasSample(dataAsset.getEntity().getHasSample())
                        .hasStatistics(dataAsset.getEntity().getHasStatistics())
                        .rowsCount(dataAsset.getEntity().getRowsCount())
                        .customAttributes(dataAsset.getEntity().getCustomAttributes().stream()
                                        .map(CustomAttribute::getSearchableArtifact).collect(Collectors.toList())).build();

                if (dataAsset.getEntity().getDomainId() != null && !dataAsset.getEntity().getDomainId().isEmpty()) {
                        Domain d = domainService.getDomainById(dataAsset.getEntity().getDomainId(), userDetails);
                        if (d != null)
                                sa.setDomainName(d.getName());
                }
                if (dataAsset.getEntity().getSystemId() != null && !dataAsset.getEntity().getSystemId().isEmpty()) {
                        System sys = systemService.getSystemById(dataAsset.getEntity().getSystemId(), userDetails);
                        if (sys != null)
                                sa.setSystemName(sys.getName());
                }
                if (dataAsset.getEntity().getEntityId() != null && !dataAsset.getEntity().getEntityId().isEmpty()) {
                        DataEntity ent = entityService.getEntityById(dataAsset.getEntity().getEntityId(), userDetails);
                        if (ent != null)
                                sa.setEntityName(ent.getName());
                }
                sa.setDomains(Collections.singletonList(dataAsset.getEntity().getDomainId()));
                return sa;
        }

        /*public EntitySampleDQRule createDQRule(String dataAssetId,
                                               UpdatableEntitySampleDQRule entitySampleDQRule, UserDetails userDetails)
                throws LottabyteException {

                EntitySampleDQRule sampleDQRule = dataAssetRepository.createDQRule(dataAssetId,
                        entitySampleDQRule, userDetails);
                return entitySampleRepository.getSampleDQRule(sampleDQRule.getId(), userDetails);
        }*/

        public void mergeDQRules(String draftId, String publishedId, List<EntitySampleDQRule> draftRules, List<EntitySampleDQRule> publishedRules, UserDetails userDetails) throws LottabyteException {

                if (publishedRules != null) {
                        for (EntitySampleDQRule rule : publishedRules) {
                                if (draftRules == null || draftRules.stream().filter(x -> x.getEntity().getAncestorId() != null && x.getEntity().getAncestorId().equals(rule.getId())).count() == 0)
                                        dqRuleService.deleteDQRuleLinkById(rule.getId(), userDetails);
                        }
                }

                if (draftRules != null) {
                        for (EntitySampleDQRule rule : draftRules) {
                                EntitySampleDQRuleEntity e = new EntitySampleDQRuleEntity();
                                e.setAssetId(publishedId);
                                e.setDqRuleId(rule.getEntity().getDqRuleId());
                                e.setPublishedId(publishedId);
                                e.setSettings(rule.getEntity().getSettings());
                                e.setAncestorId(null);
                                e.setSendMail(rule.getEntity().getSendMail());
                                e.setDisabled(rule.getEntity().getDisabled());

                                if (rule.getEntity().getAncestorId() != null)
                                        dqRuleService.patchDQRuleLinkById(rule.getEntity().getAncestorId(), new UpdatableEntitySampleDQRule(e), userDetails);
                                else
                                        dqRuleService.createDQRuleLink(e, userDetails);
                        }
                }

        }

        public void createDQRulesLinks(List<EntitySampleDQRule> dqRules, String dataAssetId, String publishedDataAssetId,
                                       UserDetails userDetails, boolean setAncestorIds) throws LottabyteException {
                Integer historyId = publishedDataAssetId == null ? 0 : dqRuleService.getLastHistoryIdByPublishedId(publishedDataAssetId, userDetails);

                if (dqRules != null && !dqRules.isEmpty()) {
                        for (EntitySampleDQRule item : dqRules) {
                                EntitySampleDQRuleEntity entity = new EntitySampleDQRuleEntity();
                                entity.setProductId(dataAssetId);
                                entity.setDisabled(item.getEntity().getDisabled());
                                entity.setDescription(item.getEntity().getDescription());
                                entity.setDqRuleId(item.getEntity().getDqRuleId());
                                entity.setSettings(item.getEntity().getSettings());
                                entity.setSendMail(item.getEntity().getSendMail());
                                entity.setPublishedId(publishedDataAssetId);
                                entity.setHistoryId(historyId);

                                if (setAncestorIds)
                                        entity.setAncestorId(item.getId());
                                else
                                        entity.setAncestorId(item.getEntity().getAncestorId());

                                UpdatableEntitySampleDQRule updatableEntitySampleDQRule = new UpdatableEntitySampleDQRule(entity);
                                dataAssetRepository.createDQRuleLink(dataAssetId, updatableEntitySampleDQRule, userDetails);
                        }
                }
        }

        private void updateDQRulesLinks(String productId, String publishedId, List<EntitySampleDQRule> newRules, List<EntitySampleDQRule> currentRules,
                                        UserDetails userDetails) {
                if (newRules != null) {
                        newRules.stream().filter(x -> EntitySampleService.containsDQRule(currentRules, x)).forEach(y -> entitySampleRepository.updateDQRuleLink(y.getId(), y, userDetails));
                        newRules.stream().filter(x -> !EntitySampleService.containsDQRule(currentRules, x))
                                .forEach(y -> dataAssetRepository.addDQRuleLink(productId, publishedId, y, userDetails));
                        if (currentRules != null) {
                                currentRules.stream().filter(x -> !EntitySampleService.containsDQRule(newRules, x))
                                        .forEach(y -> entitySampleRepository.removeDQRuleLink(y.getId(), userDetails));
                        }
                }
        }
}
