package ru.bssg.lottabyte.coreapi.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import org.flowable.engine.runtime.ProcessInstance;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.businessEntity.BusinessEntity;
import ru.bssg.lottabyte.core.model.dataasset.DataAsset;
import ru.bssg.lottabyte.core.model.dataentity.*;
import ru.bssg.lottabyte.core.model.domain.Domain;
import ru.bssg.lottabyte.core.model.product.Product;
import ru.bssg.lottabyte.core.model.reference.Reference;
import ru.bssg.lottabyte.core.model.reference.ReferenceEntity;
import ru.bssg.lottabyte.core.model.reference.ReferenceType;
import ru.bssg.lottabyte.core.model.reference.UpdatableReferenceEntity;
import ru.bssg.lottabyte.core.model.tag.Tag;
import ru.bssg.lottabyte.core.model.tag.TagEntity;
import ru.bssg.lottabyte.core.model.workflow.WorkflowTask;
import ru.bssg.lottabyte.core.model.workflow.WorkflowType;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelData;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelNodeData;
import ru.bssg.lottabyte.core.ui.model.gojs.UpdatableGojsModelData;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.*;
import ru.bssg.lottabyte.coreapi.util.Helper;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EntityService extends WorkflowableService<DataEntity> {
        private final EntityRepository entityRepository;
        private final BusinessEntityRepository businessEntityRepository;
        private final EntitySampleRepository entitySampleRepository;
        private final EntityQueryService entityQueryService;
        private final IndicatorService indicatorService;
        private final ReferenceService referenceService;
        private final ProductService productService;
        private final ProductRepository productRepository;
        private final DomainRepository domainRepository;
        private final EnumerationService enumerationService;
        private final ElasticsearchService elasticsearchService;
        private final SystemService systemService;
        private final SystemRepository systemRepository;
        private final TagService tagService;
        private final CommentService commentService;
        private final DataAssetRepository dataAssetRepository;
        private final RatingService ratingService;
        private final WorkflowService workflowService;
        private final ArtifactType serviceArtifactType = ArtifactType.entity;
        private final DomainService domainService;

        private final SearchColumn[] searchableColumns = {
                        new SearchColumn("id", SearchColumn.ColumnType.UUID),
                        new SearchColumn("name", SearchColumn.ColumnType.Text),
                        new SearchColumn("entity_folder_id", SearchColumn.ColumnType.UUID),
                        new SearchColumn("description", SearchColumn.ColumnType.Text),
                        new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
                        new SearchColumn("version_id", SearchColumn.ColumnType.Number),
                        new SearchColumn("domains", SearchColumn.ColumnType.Text),
                        new SearchColumn("systems", SearchColumn.ColumnType.Text),
                        new SearchColumn("tags", SearchColumn.ColumnType.Text),
                        new SearchColumn("workflow_state", SearchColumn.ColumnType.Text),
        };

        private final SearchColumn[] searchableAttrColumns = {
                        new SearchColumn("id", SearchColumn.ColumnType.UUID),
                        new SearchColumn("name", SearchColumn.ColumnType.Text),
                        new SearchColumn("description", SearchColumn.ColumnType.Text),
                        new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
                        new SearchColumn("version_id", SearchColumn.ColumnType.Number),
                        new SearchColumn("attribute_type", SearchColumn.ColumnType.Text),
                        new SearchColumn("attribute_type_name", SearchColumn.ColumnType.Text),
                        new SearchColumn("tags", SearchColumn.ColumnType.Text),
                        new SearchColumn("is_pk", SearchColumn.ColumnType.Number)
        };

        private final SearchColumnForJoin[] joinColumns = {
                        new SearchColumnForJoin("domain_id", "system_to_domain", SearchColumn.ColumnType.UUID,
                                        "domain_id", "id"),
                        new SearchColumnForJoin("system_id", "entity_to_system", SearchColumn.ColumnType.UUID, "id",
                                        "entity_id"),
                        new SearchColumnForJoin("source_id", "reference", SearchColumn.ColumnType.UUID, "id",
                                        "target_id"),
                        new SearchColumnForJoin("target_id", "reference", SearchColumn.ColumnType.UUID, "id",
                                        "source_id")
        };

        private final SearchColumnForJoin[] joinAttrColumns = {
        };

        @Autowired
        @Lazy
        public EntityService(EntityRepository entityRepository,
                        BusinessEntityRepository businessEntityRepository,
                        EntitySampleRepository entitySampleRepository,
                        EntityQueryService entityQueryService,
                        IndicatorService indicatorService, ReferenceService referenceService,
                        EnumerationService enumerationService,
                        ElasticsearchService elasticsearchService,
                        SystemService systemService,
                        TagService tagService,
                        CommentService commentService,
                        DataAssetRepository dataAssetRepository,
                        DomainRepository domainRepository,
                        SystemRepository systemRepository,
                        DomainService domainService,
                        RatingService ratingService,
                        ProductService productService,
                        ProductRepository productRepository,
                        WorkflowService workflowService) {
                super(entityRepository, workflowService, tagService, ArtifactType.entity, elasticsearchService);
                this.entityRepository = entityRepository;
                this.businessEntityRepository = businessEntityRepository;
                this.entitySampleRepository = entitySampleRepository;
                this.entityQueryService = entityQueryService;
                this.indicatorService = indicatorService;
                this.referenceService = referenceService;
                this.enumerationService = enumerationService;
                this.elasticsearchService = elasticsearchService;
                this.systemService = systemService;
                this.tagService = tagService;
                this.commentService = commentService;
                this.dataAssetRepository = dataAssetRepository;
                this.domainRepository = domainRepository;
                this.systemRepository = systemRepository;
                this.ratingService = ratingService;
                this.domainService = domainService;
                this.workflowService = workflowService;
                this.productService = productService;
                this.productRepository = productRepository;
        }

        // WF interface

        public boolean existsInState(String artifactId, ArtifactState artifactState, UserDetails userDetails)
                        throws LottabyteException {
                DataEntity entity = getDataEntityById(artifactId, userDetails);
                WorkflowableMetadata md = (WorkflowableMetadata) entity.getMetadata();
                if (!artifactState.equals(md.getState()))
                        return false;
                return true;
        }

        public String getDraftArtifactId(String publishedId, UserDetails userDetails) {
                return entityRepository.getDraftId(publishedId, userDetails);
        }

        public Boolean allAttributesExist(List<String> systemIds, UserDetails userDetails) {
                return entityRepository.allAttributesExist(systemIds, userDetails);
        }

        @Override
        public String createDraft(String publishedId, WorkflowState workflowState, WorkflowType workflowType,
                        UserDetails userDetails) throws LottabyteException {

                DataEntity current = getDataEntityById(publishedId, userDetails);

                ProcessInstance pi = null;
                String workflowTaskId = null;
                String draftId = UUID.randomUUID().toString();
                if (workflowService.isWorkflowEnabled(serviceArtifactType) && workflowService
                        .getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

                        pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.UPDATE,
                                userDetails);
                        workflowTaskId = pi.getId();

                }
                entityRepository.createDraftFromPublished(publishedId, draftId, workflowTaskId, userDetails);

                if (current.getEntity().getSystemIds() != null && !current.getEntity().getSystemIds().isEmpty()) {
                        for (String s : current.getEntity().getSystemIds()) {
                                entityRepository.addEntityToSystem(draftId, s, userDetails);
                        }
                }
                if (current.getEntity().getBusinessEntityId() != null
                                && !current.getEntity().getBusinessEntityId().isEmpty()) {
                        createBusinessEntityReference(current.getEntity().getBusinessEntityId(), draftId, userDetails);
                }
                mergeEntityAttributes(current.getId(), draftId, userDetails);

                tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

                return draftId;
        }

        /*
         * public DataEntity createDraft(String publishedId, WorkflowState
         * workflowState, UserDetails userDetails) throws LottabyteException {
         * DataEntity current = getDataEntityById(publishedId, userDetails);
         * String draftId = createDraftEntity(current, workflowState, userDetails);
         * return getDataEntityById(draftId, userDetails);
         * }
         */

        public void wfCancel(String draftEntityId, UserDetails userDetails) throws LottabyteException {
                DataEntity current = entityRepository.getById(draftEntityId, userDetails);
                if (current == null)
                        throw new LottabyteException(Message.LBE03004, userDetails.getLanguage(),
                                        serviceArtifactType, draftEntityId);
                if (!ArtifactState.DRAFT.equals(((WorkflowableMetadata) current.getMetadata()).getState()))
                        throw new LottabyteException(Message.LBE03003,
                                        userDetails.getLanguage());
                // deleteDomainInternal(current, userDetails);
                entityRepository.setStateById(draftEntityId, ArtifactState.DRAFT_HISTORY, userDetails);
        }

        public void wfApproveRemoval(String draftEntityId, UserDetails userDetails) throws LottabyteException {
                DataEntity current = entityRepository.getById(draftEntityId, userDetails);
                if (current == null)
                        throw new LottabyteException(
                                        Message.LBE03004,
                                                        userDetails.getLanguage(),
                                        serviceArtifactType, draftEntityId);
                String publishedId = ((WorkflowableMetadata) current.getMetadata()).getPublishedId();
                if (publishedId == null)
                        throw new LottabyteException(
                                        Message.LBE03006,
                                                        userDetails.getLanguage(),
                                        serviceArtifactType, draftEntityId);
                // Domain published = domainRepository.getDomainById(publishedId, userDetails);
                // deleteDomainInternal(current, userDetails);
                entityRepository.setStateById(current.getId(), ArtifactState.DRAFT_HISTORY, userDetails);
                // deleteDomainInternal(published, userDetails);
                entityRepository.setStateById(publishedId, ArtifactState.REMOVED, userDetails);
                referenceService.deleteReferenceBySourceId(draftEntityId, userDetails);
                elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(publishedId), userDetails);
        }

        public DataEntity wfPublish(String draftEntityId, UserDetails userDetails) throws LottabyteException {
                DataEntity draft = getDataEntityById(draftEntityId, userDetails);
                String publishedId = ((WorkflowableMetadata) draft.getMetadata()).getPublishedId();
                if (draft == null)
                        throw new LottabyteException(
                                        Message.LBE03004,
                                                        userDetails.getLanguage(),
                                        serviceArtifactType, draftEntityId);
                if (entityRepository.dataEntityNameExists(draft.getEntity().getName(),
                                draft.getEntity().getEntityFolderId(),
                                publishedId, userDetails))
                        throw new LottabyteException(
                                        Message.LBE00327,
                                                        userDetails.getLanguage(),
                                        draft.getEntity().getName());

                if (publishedId == null) {
                        String newPublishedId = entityRepository.publishEntityDraft(draftEntityId, null, userDetails);
                        if (draft.getEntity().getSystemIds() != null && !draft.getEntity().getSystemIds().isEmpty())
                                for (String s : draft.getEntity().getSystemIds())
                                        entityRepository.addEntityToSystem(newPublishedId, s, userDetails);
                        mergeEntityAttributes(draftEntityId, newPublishedId, userDetails);
                        tagService.mergeTags(draftEntityId, serviceArtifactType, newPublishedId, serviceArtifactType,
                                        userDetails);

                        if (draft.getEntity().getBusinessEntityId() != null) {
                                createBusinessEntityReference(draft.getEntity().getBusinessEntityId(), newPublishedId,
                                                userDetails);
                        }
                        DataEntity e = getDataEntityById(newPublishedId, userDetails);
                        elasticsearchService.insertElasticSearchEntity(
                                        Collections.singletonList(getSearchableArtifact(e, userDetails)), userDetails);
                        return e;
                } else {
                        entityRepository.publishEntityDraft(draftEntityId, publishedId, userDetails);
                        DataEntity currentPublished = getDataEntityById(publishedId, userDetails);
                        updateEntitySystems(publishedId, draft.getEntity().getSystemIds(),
                                        currentPublished.getEntity().getSystemIds(), userDetails);
                        mergeEntityAttributes(draftEntityId, publishedId, userDetails);

                        for (DataEntityAttribute draftAttr : entityRepository.getEntityAttributeListByEntityId(
                                        draftEntityId,
                                        userDetails))
                                entityRepository.deleteEntityAttributeToSampleProperty(draftAttr.getId(), userDetails);

                        tagService.mergeTags(draftEntityId, serviceArtifactType, publishedId, serviceArtifactType,
                                        userDetails);

                        if (referenceService.getReferenceBySourceId(publishedId, userDetails) != null)
                                referenceService.deleteReferenceBySourceId(publishedId, userDetails);

                        if (draft.getEntity().getBusinessEntityId() != null) {
                                createBusinessEntityReference(draft.getEntity().getBusinessEntityId(), publishedId,
                                                userDetails);
                        }
                        DataEntity e = getDataEntityById(publishedId, userDetails);
                        elasticsearchService.updateElasticSearchEntity(
                                        Collections.singletonList(getSearchableArtifact(e, userDetails)), userDetails);
                        return e;
                }
        }

        // Entity

        public DataEntity getById(String id, UserDetails userDetails) throws LottabyteException {
                return getDataEntityById(id, userDetails);
        }

        public boolean existEntitiesInSystem(String systemId, UserDetails userDetails) {
                return entityRepository.existEntitiesInSystem(systemId, userDetails);
        }

        public boolean hasAccessToEntity(String entityId, UserDetails userDetails) {
                return entityRepository.hasAccessToEntity(entityId, userDetails);
        }

        public DataEntity getDataEntityById(String dataEntityId, UserDetails userDetails) throws LottabyteException {
                DataEntity entity = entityRepository.getById(dataEntityId, userDetails);
                if (entity == null)
                        throw new LottabyteException(Message.LBE00301,
                                                        userDetails.getLanguage(), dataEntityId);
                WorkflowableMetadata md = (WorkflowableMetadata) entity.getMetadata();
                if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
                        md.setDraftId(entityRepository.getDraftId(md.getId(), userDetails));

                String businessEntityId = getReferenceDataAsset(dataEntityId, ArtifactType.business_entity,
                                userDetails);
                if (businessEntityId != null)
                        businessEntityRepository.getByIdAndStation(businessEntityId, ArtifactState.PUBLISHED,
                                        userDetails);

                entity.getEntity().setBusinessEntityId(businessEntityId);

                entity.getEntity().setSystemIds(entityRepository.getSystemIdsForDataEntity(
                        dataEntityId, userDetails));

                entity.getMetadata().setTags(tagService.getArtifactTags(dataEntityId, userDetails));
                return entity;
        }

        public DataEntity getDataEntityByIdAndState(String dataEntityId, ArtifactState artifactState,
                        UserDetails userDetails) throws LottabyteException {
                DataEntity entity = entityRepository.getByIdAndState(dataEntityId, artifactState.name(),
                                userDetails);
                if (entity == null)
                        throw new LottabyteException(Message.LBE00301,
                                                        userDetails.getLanguage(), dataEntityId);
                WorkflowableMetadata md = (WorkflowableMetadata) entity.getMetadata();
                if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
                        md.setDraftId(entityRepository.getDraftId(md.getId(), userDetails));

                String businessEntityId = getReferenceDataAsset(dataEntityId, ArtifactType.business_entity,
                                userDetails);
                if (businessEntityId != null)
                        businessEntityRepository.getByIdAndStation(businessEntityId, ArtifactState.PUBLISHED,
                                        userDetails);

                entity.getEntity().setBusinessEntityId(businessEntityId);

                entity.getMetadata().setTags(tagService.getArtifactTags(dataEntityId, userDetails));
                return entity;
        }

        public PaginatedArtifactList<DataEntity> getAllEntitiesPaginated(Integer offset, Integer limit,
                        String artifactState, UserDetails userDetails) throws LottabyteException {
                if (!EnumUtils.isValidEnum(ArtifactState.class, artifactState))
                        throw new LottabyteException(
                                        Message.LBE00067,
                                                        userDetails.getLanguage(),
                                        artifactState);
                PaginatedArtifactList<DataEntity> res = entityRepository.getAllPaginated(offset, limit, "/v1/entities/",
                                ArtifactState.valueOf(artifactState), userDetails);

                for (DataEntity dataEntity : res.getResources()) {
                        dataEntity.getEntity().setBusinessEntityId(
                                        getReferenceDataAsset(dataEntity.getId(), ArtifactType.business_entity,
                                                        userDetails));
                }
                res.getResources().forEach(
                                system -> system.getMetadata()
                                                .setTags(tagService.getArtifactTags(system.getId(), userDetails)));
                return res;
        }

        public String getReferenceDataAsset(String entityId, ArtifactType artifactType, UserDetails userDetails) {
                List<Reference> referenceForDataEntity = referenceService.getAllReferenceBySourceIdAndTargetType(
                                entityId,
                                String.valueOf(artifactType), userDetails);
                List<String> dataEntityIdList = referenceForDataEntity.stream().map(r -> r.getEntity().getTargetId())
                                .collect(Collectors.toList());
                return dataEntityIdList.stream().findFirst().isPresent() ? dataEntityIdList.stream().findFirst().get()
                                : null;
        }

        public DataEntity getEntityVersionById(String entityId, Integer versionId, UserDetails userDetails)
                        throws LottabyteException {
                DataEntity dataEntity = entityRepository.getVersionById(entityId, versionId, userDetails);
                if (dataEntity == null)
                        throw new LottabyteException(Message.LBE00328,
                                                        userDetails.getLanguage(), entityId, versionId);
                fillDataEntityVersionRelations(dataEntity, userDetails);

                dataEntity.getEntity()
                                .setBusinessEntityId(getReferenceDataAsset(entityId, ArtifactType.business_entity,
                                                userDetails));
                return dataEntity;
        }

        public PaginatedArtifactList<DataEntity> getEntityVersions(String entityId, Integer offset, Integer limit,
                        UserDetails userDetails) {
                if (!entityRepository.existsById(entityId,
                                new ArtifactState[] { ArtifactState.PUBLISHED, ArtifactState.REMOVED }, userDetails)) {
                        PaginatedArtifactList<DataEntity> res = new PaginatedArtifactList<>();
                        res.setCount(0);
                        res.setOffset(offset);
                        res.setLimit(limit);
                        res.setResources(new ArrayList<>());
                        return res;
                }
                PaginatedArtifactList<DataEntity> dataEntityPaginatedArtifactList = entityRepository.getVersionsById(
                                entityId,
                                offset, limit, "/v1/entities/" + entityId + "/versions", userDetails);
                for (DataEntity de : dataEntityPaginatedArtifactList.getResources()) {
                        fillDataEntityVersionRelations(de, userDetails);
                        de.getEntity()
                                        .setBusinessEntityId(getReferenceDataAsset(entityId,
                                                        ArtifactType.business_entity, userDetails));
                }
                // List<Tag> tagList = tagService.getArtifactTags(entityId, userDetails);
                // dataEntityPaginatedArtifactList.getResources().forEach(d ->
                // d.getMetadata().setTags(tagList));
                return dataEntityPaginatedArtifactList;
        }

        private void fillDataEntityVersionRelations(DataEntity de, UserDetails userDetails) {
                WorkflowableMetadata md = (WorkflowableMetadata) de.getMetadata();
                if (md.getAncestorDraftId() != null) {
                        de.getEntity()
                                        .setSystemIds(entityRepository.getSystemIdsForDataEntity(
                                                        md.getAncestorDraftId(), userDetails));
                        de.getMetadata().setTags(tagService.getArtifactTags(md.getAncestorDraftId(), userDetails));
                }
        }

        @Transactional
        public DataEntity createDataEntity(UpdatableDataEntityEntity dataEntity, UserDetails userDetails)
                        throws LottabyteException {
                // if (dataEntity.getEntityFolderId() == null ||
                // dataEntity.getEntityFolderId().isEmpty())
                // throw new LottabyteException(HttpStatus.BAD_REQUEST,
                // Message.format(Message.LBE00302));
                // if (!entityRepository.entityFolderExists(dataEntity.getEntityFolderId(),
                // userDetails))
                // throw new LottabyteException(HttpStatus.NOT_FOUND,
                // Message.format(Message.LBE00304, dataEntity.getEntityFolderId()));
                // if (entityRepository.dataEntityNameExists(dataEntity.getName(),
                // dataEntity.getEntityFolderId(), userDetails))
                // throw new LottabyteException(HttpStatus.BAD_REQUEST,
                // Message.format(Message.LBE00305, dataEntity.getName(),
                // dataEntity.getEntityFolderId()));
                if (dataEntity.getName() == null || dataEntity.getName().isEmpty())
                        throw new LottabyteException(Message.LBE00303,
                                        userDetails.getLanguage());
                if (!systemService.allSystemsExist(dataEntity.getSystemIds(), userDetails))
                        throw new LottabyteException(Message.LBE00915,
                                                        userDetails.getLanguage(),
                                                        StringUtils.join(dataEntity.getSystemIds(), ", "));
                systemService.validateAccessToSystems(dataEntity.getSystemIds(), userDetails, Message.fromString(
                                Message.LBE00322.getText(userDetails.getLanguage().name()),
                                userDetails.getLanguage().name()));

                String workflowTaskId = null;
                ProcessInstance pi = null;

                dataEntity.setId(UUID.randomUUID().toString());
                if (workflowService.isWorkflowEnabled(serviceArtifactType)
                                && workflowService.getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH,
                                                userDetails) != null) {

                        pi = workflowService.startFlowableProcess(dataEntity.getId(), serviceArtifactType,
                                        ArtifactAction.CREATE,
                                        userDetails);
                        workflowTaskId = pi.getId();

                }

                String newEntityId = entityRepository.createDataEntity(dataEntity, workflowTaskId, userDetails);
                if (dataEntity.getBusinessEntityId() != null) {
                        createBusinessEntityReference(dataEntity.getBusinessEntityId(), newEntityId, userDetails);
                }
                DataEntity entity = getDataEntityById(newEntityId, userDetails);

                // elasticsearchService.insertElasticSearchEntity(Collections.singletonList(entity.getSearchableArtifact()),
                // userDetails);
                return entity;
        }

        public void createBusinessEntityReference(String businessEntityId, String newEntityId, UserDetails userDetails)
                        throws LottabyteException {
                if (businessEntityId != null) {
                        ReferenceEntity referenceEntity = new ReferenceEntity();
                        referenceEntity.setSourceId(newEntityId);
                        referenceEntity.setSourceType(ArtifactType.entity);
                        referenceEntity.setTargetId(businessEntityId);
                        referenceEntity.setTargetType(ArtifactType.business_entity);
                        referenceEntity.setReferenceType(ReferenceType.DATA_ENTITY_TO_BUSINESS_ENTITY);

                        UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                        referenceService.createReference(newReferenceEntity, userDetails);
                }
        }

        public DataEntity updateDataEntity(String dataEntityId, UpdatableDataEntityEntity dataEntity,
                        UserDetails userDetails) throws LottabyteException {
                if (!entityRepository.existsById(dataEntityId,
                                new ArtifactState[] { ArtifactState.PUBLISHED, ArtifactState.DRAFT }, userDetails))
                        throw new LottabyteException(Message.LBE00301,
                                                        userDetails.getLanguage(), dataEntityId);
                /*
                 * if (userDetails.getStewardId() != null &&
                 * !entityRepository.hasAccessToEntity(dataEntityId, userDetails))
                 * throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.LBE00324,
                 * dataEntityId);
                 */

                DataEntity current = getDataEntityById(dataEntityId, userDetails);
                String draftId = null;
                if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
                        draftId = entityRepository.getDraftId(dataEntityId, userDetails);
                        if (draftId != null && !draftId.isEmpty())
                                throw new LottabyteException(
                                                                Message.LBE00326,
                                                                userDetails.getLanguage(),
                                                draftId);
                }

                if (dataEntity.getEntityFolderId() != null
                                && entityRepository.dataEntityNameExists(dataEntity.getName(),
                                                dataEntity.getEntityFolderId(), dataEntityId, userDetails))
                        throw new LottabyteException(Message.LBE00305,
                                                        userDetails.getLanguage(), dataEntity.getName(),
                                                        dataEntity.getEntityFolderId());
                if (!systemService.allSystemsExist(dataEntity.getSystemIds(), userDetails))
                        throw new LottabyteException(Message.LBE00915,
                                                        userDetails.getLanguage(),
                                                        StringUtils.join(dataEntity.getSystemIds(), ", "));
                validateSystemIds(dataEntityId, dataEntity, current.getEntity(), userDetails);

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
                        entityRepository.createEntityDraft(dataEntityId, draftId, workflowTaskId, userDetails);
                        if (current.getEntity().getSystemIds() != null
                                        && !current.getEntity().getSystemIds().isEmpty()) {
                                for (String s : current.getEntity().getSystemIds())
                                        entityRepository.addEntityToSystem(draftId, s, userDetails);
                        }
                        if (current.getEntity().getBusinessEntityId() != null
                                        && !current.getEntity().getBusinessEntityId().isEmpty()) {
                                createBusinessEntityReference(current.getEntity().getBusinessEntityId(), draftId,
                                                userDetails);
                        }
                        mergeEntityAttributes(current.getId(), draftId, userDetails);
                        tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType,
                                        userDetails);

                } else

                {
                        draftId = dataEntityId;
                }

                if (current.getEntity().getBusinessEntityId() != null) {
                        if (dataEntity.getBusinessEntityId() != null) {
                                referenceService.deleteByReferenceSourceIdAndTargetId(draftId,
                                                current.getEntity().getBusinessEntityId(), userDetails);
                                createBusinessEntityReference(dataEntity.getBusinessEntityId(), draftId, userDetails);
                        }
                } else {
                        if (dataEntity.getBusinessEntityId() != null) {
                                createBusinessEntityReference(dataEntity.getBusinessEntityId(), draftId, userDetails);
                        }
                }

                // DataEntity entity =
                entityRepository.updateDataEntity(draftId, dataEntity, userDetails);
                // elasticsearchService.updateElasticSearchEntity(Collections.singletonList(entity.getSearchableArtifact()),
                // userDetails);
                return

                getDataEntityById(draftId, userDetails);
        }

        private void mergeEntityAttributes(String sourceEntityId, String targetEntityId, UserDetails userDetails)
                        throws LottabyteException {
                List<DataEntityAttribute> source = entityRepository.getEntityAttributeListByEntityId(sourceEntityId,
                                userDetails);
                List<DataEntityAttribute> target = entityRepository.getEntityAttributeListByEntityId(targetEntityId,
                                userDetails);
                for (DataEntityAttribute a : source) {
                        Optional<DataEntityAttribute> tAttr = target.stream()
                                        .filter(x -> x.getEntity().getAttributeId()
                                                        .equals(a.getEntity().getAttributeId()))
                                        .findFirst();
                        if (!tAttr.isPresent()) {
                                // TODO redo to copy attribute
                                String newAttributeId = entityRepository.createEntityAttribute(targetEntityId,
                                                new UpdatableDataEntityAttributeEntity(a.getEntity()), userDetails);
                                List<Tag> attrTags = tagService.getArtifactTags(a.getId(), userDetails);
                                if (attrTags != null) {
                                        for (Tag tag : attrTags) {
                                                tagService.linkToArtifact(newAttributeId,
                                                                String.valueOf(ArtifactType.entity_attribute),
                                                                tag.getEntity(), userDetails);
                                        }
                                }
                                entityRepository.copyEntityAttributeToSamplePropertyLinks(a.getId(), newAttributeId,
                                                userDetails);
                        } else {
                                DataEntityAttributeEntity ent = new DataEntityAttributeEntity();
                                ent.setAttributeType(a.getEntity().getAttributeType());
                                ent.setDescription(a.getEntity().getDescription());
                                ent.setName(a.getEntity().getName());
                                ent.setEntityId(targetEntityId);
                                ent.setId(tAttr.get().getId());
                                ent.setIsPk(a.getEntity().getIsPk());
                                entityRepository.patchEntityAttribute(tAttr.get().getId(),
                                                new UpdatableDataEntityAttributeEntity(ent),
                                                userDetails);
                                tagService.mergeTags(a.getId(), ArtifactType.entity_attribute, tAttr.get().getId(),
                                                ArtifactType.entity_attribute, userDetails);
                        }
                }
                for (DataEntityAttribute a : target) {
                        if (!source.stream().anyMatch(
                                        x -> x.getEntity().getAttributeId().equals(a.getEntity().getAttributeId()))) {
                                entityRepository.deleteEntityAttribute(a.getId(), true, userDetails);
                        }
                }
        }

        private void validateSystemIds(String dataEntityId, UpdatableDataEntityEntity dataEntityEntity,
                        DataEntityEntity currentEntity, UserDetails userDetails) throws LottabyteException {
                List<String> currentSystems = new ArrayList<>();
                if (currentEntity != null && currentEntity.getSystemIds() != null)
                        currentSystems = currentEntity.getSystemIds();
                if (dataEntityEntity.getSystemIds() != null) {
                        systemService.validateAccessToSystems(dataEntityEntity.getSystemIds(), userDetails,
                                        Message.fromString(
                                                        Message.LBE00322.getText(userDetails.getLanguage().name()),
                                                        userDetails.getLanguage().name()));
                        List<String> erroredSystems = new ArrayList<>();
                        for (String s : dataEntityEntity.getSystemIds()) {
                                if (!systemRepository.existsByIdAndPublished(s, userDetails))
                                        erroredSystems.add(s);
                        }
                        if (!erroredSystems.isEmpty())
                                throw new LottabyteException(
                                                                Message.LBE00114,
                                                                userDetails.getLanguage(),
                                                String.join(", ", erroredSystems));
                        List<String> systemsIdsToDelete = currentSystems.stream()
                                        .filter(x -> !dataEntityEntity.getSystemIds().contains(x))
                                        .collect(Collectors.toList());
                        systemService.validateAccessToSystems(systemsIdsToDelete, userDetails, Message.fromString(
                                        Message.LBE00323.getText(userDetails.getLanguage().name()),
                                        userDetails.getLanguage().name()));
                        if (systemsIdsToDelete.stream().anyMatch(
                                        x -> dataAssetRepository.existsDataAssetWithSystemAndEntity(x, dataEntityId,
                                                        userDetails))) {
                                List<String> errors = new ArrayList<>();
                                List<String> erroredSystemsIds = new ArrayList<>();
                                for (String systemId : systemsIdsToDelete) {
                                        List<DataAsset> dataAssets = dataAssetRepository.getDataAssetsBySystemAndEntity(
                                                        systemId,
                                                        dataEntityId, userDetails);
                                        if (!dataAssets.isEmpty()) {
                                                dataAssets.forEach(
                                                                x -> errors.add(x.getId() + " (" + x.getName() + ")"));
                                                erroredSystemsIds.add(systemId);
                                        }
                                }
                                throw new LottabyteException(Message.LBE00314,
                                                                userDetails.getLanguage(),
                                                                String.join(", ", erroredSystemsIds),
                                                                String.join(", ", errors));
                        }
                }
        }

        @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
        public DataEntity deleteDataEntityById(String dataEntityId, UserDetails userDetails) throws LottabyteException {
                if (!entityRepository.existsById(dataEntityId,
                                new ArtifactState[] { ArtifactState.PUBLISHED, ArtifactState.DRAFT }, userDetails))
                        throw new LottabyteException(Message.LBE00301,
                                                        userDetails.getLanguage(), dataEntityId);
                /*
                 * if (userDetails.getStewardId() != null &&
                 * !entityRepository.hasAccessToEntity(dataEntityId, userDetails))
                 * throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.LBE00324,
                 * dataEntityId);
                 */

                DataEntity current = getDataEntityById(dataEntityId, userDetails);
                if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
                        if (dataAssetRepository.existsDataAssetWithEntity(dataEntityId, userDetails))
                                throw new LottabyteException(
                                                Message.LBE00315,
                                                userDetails.getLanguage());
                        /*
                         * if (!entityRepository.getSystemIdsForDataEntity(dataEntityId,
                         * userDetails).isEmpty())
                         * throw new LottabyteException(HttpStatus.BAD_REQUEST,
                         * Message.format(Message.LBE00316, dataEntityId));
                         */
                        if (!entityQueryService.getEntityQueryListByEntityId(dataEntityId, userDetails).isEmpty())
                                throw new LottabyteException(Message.LBE00317,
                                                                userDetails.getLanguage(), dataEntityId);
                        if (entitySampleRepository.existsEntitySampleWithEntity(dataEntityId, userDetails))
                                throw new LottabyteException(
                                                Message.LBE00318,
                                                userDetails.getLanguage());
                        List<DataEntityAttribute> dataEntityAttributeList = entityRepository
                                        .getEntityAttributeListByEntityId(dataEntityId, userDetails);
                        for (DataEntityAttribute dataEntityAttribute : dataEntityAttributeList) {
                                if (entitySampleRepository.existsSamplePropertyByEntityAttributeId(
                                                dataEntityAttribute.getId(),
                                                userDetails))
                                        throw new LottabyteException(Message.LBE00319,
                                                                        userDetails.getLanguage(),
                                                                        dataEntityAttribute.getId());
                        }
                        if (current.getEntity().getSystemIds() != null)
                                systemService.validateAccessToSystems(current.getEntity().getSystemIds(), userDetails,
                                                Message.fromString(
                                                                Message.LBE00323.getText(
                                                                                userDetails.getLanguage().name()),
                                                                userDetails.getLanguage().name()));
                        String draftId = entityRepository.getDraftId(dataEntityId, userDetails);
                        if (draftId != null && !draftId.isEmpty())
                                throw new LottabyteException(
                                                                Message.LBE00326,
                                                                userDetails.getLanguage(),
                                                draftId);

                        ProcessInstance pi = null;
                        draftId = null;
                        String workflowTaskId = null;

                        draftId = UUID.randomUUID().toString();
                        pi = workflowService.startFlowableProcess(draftId, serviceArtifactType,
                                        ArtifactAction.REMOVE, userDetails);
                        workflowTaskId = pi.getId();

                        draftId = entityRepository.createEntityDraft(current.getId(), draftId, workflowTaskId,
                                        userDetails);

                        // draftId = createDraftEntity(current, WorkflowState.MARKED_FOR_REMOVAL,
                        // userDetails);
                        if (current.getEntity().getSystemIds() != null && !current.getEntity().getSystemIds().isEmpty())
                                for (String s : current.getEntity().getSystemIds())
                                        entityRepository.addEntityToSystem(draftId, s, userDetails);
                        mergeEntityAttributes(current.getId(), draftId, userDetails);
                        tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType,
                                        userDetails);
                        createBusinessEntityReference(current.getEntity().getBusinessEntityId(), draftId, userDetails);

                        return getEntityById(draftId, userDetails);
                } else {
                        List<Reference> indicatorReferenceList = referenceService
                                        .getAllReferenceBySourceIdAndTargetType(
                                                        dataEntityId, String.valueOf(ArtifactType.entity_attribute),
                                                        userDetails);

                        for (Reference reference : indicatorReferenceList) {
                                List<String> indicatorIdList = indicatorService
                                                .entityAttributeExistInAllFormulas(reference.getEntity().getTargetId(),
                                                                userDetails);
                                if (indicatorIdList != null && !indicatorIdList.isEmpty())
                                        throw new LottabyteException(Message.LBE00329,
                                                                        userDetails.getLanguage(),
                                                                        reference.getEntity().getTargetId());
                        }

                        referenceService.deleteReferenceBySourceId(dataEntityId, userDetails);
                        tagService.deleteAllTagsByArtifactId(dataEntityId, userDetails);
                        entityRepository.deleteDataEntityById(dataEntityId, userDetails);
                        // TODO: remove links
                        return null;
                }
        }

        private String createDraftEntity(DataEntity current, WorkflowState workflowState, UserDetails userDetails) {
                String workflowTaskId = workflowService.getNewWorkflowTaskUUID().toString();
                String draftId = entityRepository.createEntityDraft(current.getId(), null, workflowTaskId, userDetails);
                /*
                 * if (current.getEntity().getSystemIds() != null &&
                 * !current.getEntity().getSystemIds().isEmpty())
                 * for (String s : current.getEntity().getSystemIds())
                 * entityRepository.addEntityToSystem(draftId, s, userDetails);
                 * tagService.mergeTags(current.getId(), serviceArtifactType, draftId,
                 * serviceArtifactType, userDetails);
                 */
 
                return draftId;
        }

        public Boolean existEntityByIdAndState(String entityId, UserDetails userDetails) {
                return entityRepository.existsById(entityId, new ArtifactState[] { ArtifactState.PUBLISHED }, userDetails);
        }

        public DataEntity getEntityById(String entityId, UserDetails userDetails) {
                return entityRepository.getById(entityId, userDetails);
        }

        public DataEntityFolder createFolder(UpdatableDataEntityFolderEntity newDataEntityFolderEntity,
                        UserDetails userDetails) throws LottabyteException {
                if (newDataEntityFolderEntity.getParentId() == null
                                || !entityRepository.existEntityFolderById(newDataEntityFolderEntity.getParentId(),
                                                userDetails)) {
                        throw new LottabyteException(Message.LBE00306,
                                                        userDetails.getLanguage(),
                                                        newDataEntityFolderEntity.getParentId());
                }
                if (newDataEntityFolderEntity.getName() == null
                                || getDataEntityFolderByNameAndParent(newDataEntityFolderEntity.getParentId(),
                                                newDataEntityFolderEntity.getName(), false, userDetails) != null) {
                        throw new LottabyteException(Message.LBE00307,
                                                        userDetails.getLanguage(),
                                                        newDataEntityFolderEntity.getName(),
                                                        newDataEntityFolderEntity.getParentId());
                }
                String folderId = entityRepository.createFolder(newDataEntityFolderEntity, userDetails);
                DataEntityFolder dataEntityFolder = getDataEntityFolderById(folderId, false, userDetails);
                elasticsearchService.insertElasticSearchEntity(
                                Collections.singletonList(getFolderSearchableArtifact(dataEntityFolder, userDetails)),
                                userDetails);
                return dataEntityFolder;
        }

        public DataEntityFolder patchFolder(String folderId, UpdatableDataEntityFolderEntity dataEntityFolderEntity,
                        UserDetails userDetails) throws LottabyteException {
                if (folderId == null || folderId.isEmpty()) {
                        throw new LottabyteException(
                                        Message.LBE00308,
                                        userDetails.getLanguage());
                }
                if (!entityRepository.existEntityFolderById(folderId, userDetails)) {
                        throw new LottabyteException(Message.LBE00304,
                                                        userDetails.getLanguage(), folderId);
                }
                if (dataEntityFolderEntity.getParentId() == null
                                || !entityRepository.existEntityFolderById(dataEntityFolderEntity.getParentId(),
                                                userDetails)) {
                        throw new LottabyteException(Message.LBE00306,
                                                        userDetails.getLanguage(),
                                                        dataEntityFolderEntity.getParentId());
                }
                if (dataEntityFolderEntity.getName() == null
                                || getDataEntityFolderByNameAndParent(dataEntityFolderEntity.getParentId(),
                                                dataEntityFolderEntity.getName(), false, userDetails) != null) {
                        throw new LottabyteException(Message.LBE00307,
                                                        userDetails.getLanguage(),
                                                        dataEntityFolderEntity.getName(),
                                                        dataEntityFolderEntity.getParentId());
                }
                List<String> childrenList = entityRepository.getChildrenIds(new ArrayList<>(), folderId, userDetails);
                if (dataEntityFolderEntity.getParentId() == null || childrenList.contains(folderId)) {
                        throw new LottabyteException(Message.LBE00309,
                                                        userDetails.getLanguage(),
                                                        dataEntityFolderEntity.getParentId(), folderId);
                }

                entityRepository.patchFolder(folderId, dataEntityFolderEntity, userDetails);
                DataEntityFolder dataEntityFolder = getDataEntityFolderById(folderId, false, userDetails);
                elasticsearchService.updateElasticSearchEntity(
                                Collections.singletonList(getFolderSearchableArtifact(dataEntityFolder, userDetails)),
                                userDetails);
                return dataEntityFolder;
        }

        public DataEntityFolder getDataEntityFolderByNameAndParent(String parentId, String entityFolderName,
                        Boolean includeChildren, UserDetails userDetails) throws LottabyteException {
                return entityRepository.getDataEntityFolderByNameAndParent(parentId, entityFolderName, includeChildren,
                                userDetails);
        }

        public DataEntityFolder getDataEntityFolderById(String entityFolderId, Boolean includeChildren,
                        UserDetails userDetails) throws LottabyteException {
                DataEntityFolder dataEntityFolder = entityRepository.getDataEntityFolderById(entityFolderId,
                                includeChildren,
                                userDetails);
                if (dataEntityFolder == null)
                        throw new LottabyteException(Message.LBE00304,
                                                        userDetails.getLanguage(), entityFolderId);
                return dataEntityFolder;
        }

        public ArchiveResponse deleteFolder(String folderId, UserDetails userDetails) throws LottabyteException {
                if (folderId == null || folderId.isEmpty()) {
                        throw new LottabyteException(
                                        Message.LBE00308,
                                        userDetails.getLanguage());
                }
                if (getDataEntityFolderById(folderId, false, userDetails) == null) {
                        throw new LottabyteException(Message.LBE00304,
                                                        userDetails.getLanguage(), folderId);
                }
                ArchiveResponse archiveResponse = new ArchiveResponse();
                List<DataEntityFolder> dataEntityFolderList = getDataEntityFolderWithAllChildrenById(folderId,
                                userDetails);
                if (dataEntityFolderList != null && !dataEntityFolderList.isEmpty()) {
                        for (DataEntityFolder dataEntityFolder : dataEntityFolderList) {
                                if (existsDataEntityByEntityFolderId(dataEntityFolder.getId(), userDetails))
                                        throw new LottabyteException(Message.LBE00310,
                                                                        userDetails.getLanguage(), folderId);
                        }
                        entityRepository.deletionFolders(dataEntityFolderList, userDetails);
                        List<String> idList = dataEntityFolderList.stream().map(ModeledObject::getId)
                                        .collect(Collectors.toList());

                        elasticsearchService.deleteElasticSearchEntityById(idList, userDetails);

                        archiveResponse.setArchivedGuids(idList);
                }

                return archiveResponse;
        }

        public boolean existsDataEntityByEntityFolderId(String dataEntityFolderId, UserDetails userDetails) {
                return entityRepository.existsDataEntityByEntityFolderId(dataEntityFolderId, userDetails);
        }

        public List<DataEntityFolder> getDataEntityFolderWithAllChildrenById(String dataEntityFolderId,
                        UserDetails userDetails) {
                return entityRepository.getDataEntityFolderWithAllChildrenById(dataEntityFolderId, userDetails);
        }

        public List<DataEntityFolder> getRootFolders(Boolean includeChildren, UserDetails userDetails) {
                return entityRepository.getRootFolders(includeChildren, userDetails);
        }

        public PaginatedArtifactList<DataEntityAttribute> getEntityAttributesWithPaging(String entityId, Integer offset,
                        Integer limit, UserDetails userDetails) throws LottabyteException {
                if (entityId == null || entityId.isEmpty()) {
                        throw new LottabyteException(Message.LBE00311,
                                        userDetails.getLanguage());
                }
                if (!entityRepository.existsById(entityId, userDetails)) {
                        throw new LottabyteException(Message.LBE00301,
                                                        userDetails.getLanguage(), entityId);
                }

                return entityRepository.getEntityAttributesWithPaging(entityId, offset, limit, userDetails);
        }

        public DataEntityAttribute getEntityAttributeById(String entityAttributeId, UserDetails userDetails)
                        throws LottabyteException {
                if (entityAttributeId == null || entityAttributeId.isEmpty()) {
                        throw new LottabyteException(
                                        Message.LBE00313,
                                        userDetails.getLanguage());
                }
                if (!entityRepository.existEntityAttributeById(entityAttributeId, userDetails)) {
                        throw new LottabyteException(Message.LBE00312,
                                                        userDetails.getLanguage(), entityAttributeId);
                }

                return entityRepository.getEntityAttributeById(entityAttributeId, userDetails);
        }

        public DataEntityAttribute createEntityAttribute(String entityId,
                        UpdatableDataEntityAttributeEntity newDataEntityAttributeEntity, UserDetails userDetails)
                        throws LottabyteException {
                DataEntity entity = getEntityById(entityId, userDetails);
                WorkflowableMetadata md = (WorkflowableMetadata) entity.getMetadata();
                if (ArtifactState.PUBLISHED.equals(md.getState()))
                        throw new LottabyteException(
                                        Message.LBE02209,
                                        userDetails.getLanguage());
                if (newDataEntityAttributeEntity.getName() == null ||
                                entityRepository.existEntityAttributeByName(entityId,
                                                newDataEntityAttributeEntity.getName(), null,
                                                userDetails)) {
                        throw new LottabyteException(Message.LBE02201,
                                                        userDetails.getLanguage(),
                                                        newDataEntityAttributeEntity.getName());
                }
                if (newDataEntityAttributeEntity.getAttributeType() == null || entityRepository
                                .dataEntityAttributeTypeMap(
                                                newDataEntityAttributeEntity.getAttributeType().toString()) == null) {
                        throw new LottabyteException(Message.LBE02202,
                                                        userDetails.getLanguage(),
                                                        newDataEntityAttributeEntity.getAttributeType());
                }
                if (newDataEntityAttributeEntity.getEnumerationId() != null) {
                        if (enumerationService.getEnumerationById(newDataEntityAttributeEntity.getEnumerationId(),
                                        userDetails) == null) {
                                throw new LottabyteException(Message.LBE02701,
                                                                userDetails.getLanguage(),
                                                                newDataEntityAttributeEntity.getEnumerationId());
                        }
                        if (!newDataEntityAttributeEntity.getAttributeType()
                                        .equals(DataEntityAttributeType.ENUMERATION)) {
                                throw new LottabyteException(
                                                Message.LBE02702,
                                                userDetails.getLanguage());
                        }
                }
                if (userDetails.getStewardId() != null && !entityRepository.hasAccessToEntity(entityId, userDetails))
                        throw new LottabyteException(
                                        Message.LBE02205,
                                                        userDetails.getLanguage(),
                                        entityId);

                String attributeId = entityRepository.createEntityAttribute(entityId, newDataEntityAttributeEntity,
                                userDetails);
                DataEntityAttribute dataEntityAttribute = getEntityAttributeById(attributeId, userDetails);
                elasticsearchService.insertElasticSearchEntity(
                                Collections.singletonList(
                                                getAttributeSearchableArtifact(dataEntityAttribute, userDetails)),
                                userDetails);
                return dataEntityAttribute;
        }

        public DataEntityAttribute patchEntityAttribute(String entityAttributeId,
                        UpdatableDataEntityAttributeEntity dataEntityAttributeEntity, UserDetails userDetails)
                        throws LottabyteException {
                if (!entityRepository.existEntityAttributeById(entityAttributeId, userDetails))
                        throw new LottabyteException(
                                        Message.LBE02203,
                                                        userDetails.getLanguage(),
                                        entityAttributeId);
                if (dataEntityAttributeEntity.getAttributeType() != null && entityRepository
                                .dataEntityAttributeTypeMap(
                                                dataEntityAttributeEntity.getAttributeType().toString()) == null)
                        throw new LottabyteException(
                                        Message.LBE02202,
                                                        userDetails.getLanguage(),
                                        dataEntityAttributeEntity.getAttributeType());
                DataEntityAttribute current = entityRepository.getEntityAttributeById(entityAttributeId, userDetails);
                DataEntity entity = getEntityById(current.getEntity().getEntityId(), userDetails);
                WorkflowableMetadata md = (WorkflowableMetadata) entity.getMetadata();
                if (ArtifactState.PUBLISHED.equals(md.getState()))
                        throw new LottabyteException(
                                        Message.LBE02209,
                                        userDetails.getLanguage());
                if (dataEntityAttributeEntity.getEntityId() != null
                                && !current.getEntity().getEntityId().equals(dataEntityAttributeEntity.getEntityId()))
                        throw new LottabyteException(
                                        Message.LBE02208,
                                                        userDetails.getLanguage(),
                                        entityAttributeId);
                if (userDetails.getStewardId() != null
                                && !entityRepository.hasAccessToEntity(current.getEntity().getEntityId(), userDetails))
                        throw new LottabyteException(
                                        Message.LBE02206,
                                                        userDetails.getLanguage(),
                                        current.getEntity().getEntityId());
                if (dataEntityAttributeEntity.getEnumerationId() != null) {
                        if (enumerationService.getEnumerationById(dataEntityAttributeEntity.getEnumerationId(),
                                        userDetails) == null) {
                                throw new LottabyteException(
                                                                Message.LBE02701,
                                                                userDetails.getLanguage(),
                                                dataEntityAttributeEntity.getEnumerationId());
                        }
                        if (!dataEntityAttributeEntity.getAttributeType().equals(DataEntityAttributeType.ENUMERATION)) {
                                throw new LottabyteException(
                                                Message.LBE02702,
                                                userDetails.getLanguage());
                        }
                }
                if (dataEntityAttributeEntity.getName() != null &&
                                entityRepository.existEntityAttributeByName(current.getEntity().getEntityId(),
                                                dataEntityAttributeEntity.getName(),
                                                entityAttributeId, userDetails))
                        throw new LottabyteException(
                                        Message.LBE02204,
                                                        userDetails.getLanguage(),
                                        dataEntityAttributeEntity.getName(), current.getEntity().getEntityId());

                entityRepository.patchEntityAttribute(entityAttributeId, dataEntityAttributeEntity, userDetails);

                DataEntityAttribute entityAttribute = getEntityAttributeById(entityAttributeId, userDetails);

                if (dataEntityAttributeEntity.getTags() != null) {
                        List<Tag> currentTags = tagService.getArtifactTags(entityAttributeId, userDetails);
                        for (Tag tag : currentTags) {
                                if (!dataEntityAttributeEntity.getTags().contains(tag.getName()))
                                        tagService.unlinkFromArtifact(entityAttributeId,
                                                        String.valueOf(ArtifactType.entity_attribute),
                                                        tag.getEntity(), userDetails);
                        }

                        for (String tagName : dataEntityAttributeEntity.getTags()) {
                                if (currentTags.stream().filter(x -> x.getName().equals(tagName)).count() == 0) {
                                        TagEntity tagEntity = new TagEntity();
                                        tagEntity.setName(tagName);
                                        tagService.linkToArtifact(entityAttributeId,
                                                        String.valueOf(ArtifactType.entity_attribute),
                                                        tagEntity, userDetails);
                                }
                        }
                }

                elasticsearchService.updateElasticSearchEntity(
                                Collections.singletonList(getAttributeSearchableArtifact(entityAttribute, userDetails)),
                                userDetails);
                return entityAttribute;
        }

        public ArchiveResponse deleteEntityAttribute(String entityAttributeId, Boolean force, UserDetails userDetails)
                        throws LottabyteException {
                if (entityAttributeId == null || entityAttributeId.isEmpty())
                        throw new LottabyteException(Message.LBE00313,
                                        userDetails.getLanguage());
                DataEntityAttribute attribute = entityRepository.getEntityAttributeById(entityAttributeId, userDetails);
                if (attribute == null)
                        throw new LottabyteException(
                                        Message.LBE00312,
                                                        userDetails.getLanguage(),
                                        entityAttributeId);
                DataEntity entity = getEntityById(attribute.getEntity().getEntityId(), userDetails);
                WorkflowableMetadata md = (WorkflowableMetadata) entity.getMetadata();
                if (ArtifactState.PUBLISHED.equals(md.getState()))
                        throw new LottabyteException(
                                        Message.LBE02209,
                                        userDetails.getLanguage());

                DataEntityAttribute current = getEntityAttributeById(entityAttributeId, userDetails);
                if (userDetails.getStewardId() != null) {
                        if (!entityRepository.hasAccessToEntity(current.getEntity().getEntityId(), userDetails))
                                throw new LottabyteException(
                                                                Message.LBE02207,
                                                                userDetails.getLanguage(),
                                                current.getEntity().getEntityId());
                }
                if (!force) {
                        String publishedAttributeId = null;
                        for (DataEntityAttribute publishedAttr : entityRepository
                                        .getEntityAttributeListByEntityId(md.getPublishedId(), userDetails)) {
                                if (publishedAttr.getEntity().getAttributeId()
                                                .equals(current.getEntity().getAttributeId()))
                                        publishedAttributeId = publishedAttr.getId();
                        }

                        if (entitySampleRepository.existsSamplePropertyByEntityAttributeId(publishedAttributeId,
                                        userDetails))
                                throw new LottabyteException(Message.LBE00330,
                                                                userDetails.getLanguage(), entityAttributeId);
                        List<Reference> refs = referenceService.getAllReferencesByTargetIdAndRefType(
                                        publishedAttributeId,
                                        ReferenceType.PRODUCT_TO_DATA_ENTITY_ATTRIBUTE, userDetails);
                        if (!refs.isEmpty()) {
                                List<String> parts = new ArrayList<>();
                                List<Reference> deleteRefs = new ArrayList<>();
                                for (Reference ref : refs) {
                                        Product p = productRepository.getById(ref.getEntity().getSourceId(),
                                                        userDetails);
                                        if (p != null
                                                        && ((WorkflowableMetadata) p.getMetadata()).getState()
                                                                        .equals(ArtifactState.PUBLISHED))
                                                parts.add("<a href=\"/products/edit/" + p.getId() + "\">" + p.getName()
                                                                + "</a>");
                                        else
                                                deleteRefs.add(ref);
                                }
                                if (parts.size() > 0)
                                        throw new LottabyteException(HttpStatus.BAD_REQUEST,
                                                        Message.LBE00331.getText(userDetails.getLanguage().name())
                                                                        + StringUtils.join(parts, ", "));
                                else {
                                        for (Reference ref : deleteRefs)
                                                referenceService.deleteReferenceById(ref.getId(), userDetails);
                                }
                        }
                }
                List<String> indicatorIdList = indicatorService.entityAttributeExistInAllFormulas(entityAttributeId,
                                userDetails);
                if (md.getPublishedId() != null) {
                        List<DataEntityAttribute> publishedEntityAttribs = entityRepository
                                        .getEntityAttributeListByEntityId(md.getPublishedId(), userDetails);
                        for (DataEntityAttribute dea : publishedEntityAttribs) {
                                if (dea.getEntity().getAttributeId().equals(current.getEntity().getAttributeId())) {
                                        indicatorIdList
                                                        .addAll(indicatorService.entityAttributeExistInAllFormulas(
                                                                        dea.getId(), userDetails));
                                }
                        }
                }
                if (indicatorIdList != null && !indicatorIdList.isEmpty())
                        throw new LottabyteException(HttpStatus.BAD_REQUEST,
                                        Message.format(Message.LBE00329.getText(userDetails.getLanguage().name()),
                                                        userDetails.getLanguage().name(), entityAttributeId));

                ArchiveResponse archiveResponse = new ArchiveResponse();
                entityRepository.deleteEntityAttribute(entityAttributeId, force, userDetails);
                tagService.deleteAllTagsByArtifactId(entityAttributeId, userDetails);
                commentService.deleteAllCommentsByArtifactId(entityAttributeId, userDetails);
                List<String> idList = new ArrayList<>(Collections.singletonList(entityAttributeId));

                elasticsearchService.deleteElasticSearchEntityById(idList, userDetails);
                archiveResponse.setArchivedGuids(idList);
                return archiveResponse;
        }

        /*
         * public EntityModel getEntityModelById(String entityId, UserDetails
         * userDetails) {
         * return entityRepository.getEntityModelById(entityId, userDetails);
         * }
         */

        public SearchResponse<FlatDataEntity> searchDataEntities(SearchRequestWithJoin request, UserDetails userDetails)
                        throws LottabyteException {
                ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
                SearchResponse<FlatDataEntity> res = entityRepository.searchDataEntities(request, searchableColumns,
                                joinColumns, userDetails);
                for (FlatDataEntity item : res.getItems()) {
                        item.setSystems(systemService.getSystemsByEntityId(item.getId(), userDetails).stream()
                                        .map(y -> FlatRelation.builder()
                                                        .id(y.getId()).name(y.getName()).url("/v1/systems/" + y.getId())
                                                        .build())
                                        .collect(Collectors.toList()));
                        Set<String> domainIds = new HashSet<>();
                        item.setDomains(new ArrayList<>());
                        for (FlatRelation s : item.getSystems()) {
                                List<Domain> domains = domainService.getDomainsBySystemId(s.getId(), userDetails);
                                for (Domain d : domains) {
                                        if (!domainIds.contains(d.getId())) {
                                                item.getDomains().add(FlatRelation.builder().id(d.getId())
                                                                .name(d.getName())
                                                                .url("/v1/domains/" + d.getId()).build());
                                                domainIds.add(d.getId());
                                        }
                                }
                        }
                }
                for (FlatDataEntity flatDataEntity : res.getItems()) {
                        flatDataEntity.setBusinessEntityId(
                                        getReferenceDataAsset(flatDataEntity.getId(), ArtifactType.business_entity,
                                                        userDetails));
                }
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

        public SearchResponse<FlatDataEntity> searchDataEntityByDomain(SearchRequestWithJoin request, String domainId,
                        UserDetails userDetails) throws LottabyteException {
                ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
                if (domainId == null || domainId.isEmpty() || !domainId
                                .matches("[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}")) {
                        throw new LottabyteException(Message.LBE00101,
                                                        userDetails.getLanguage(), domainId);
                }
                if (request.getLimitSteward() != null && request.getLimitSteward() && userDetails.getStewardId() != null
                                &&
                                !domainRepository.hasAccessToDomain(domainId, userDetails))
                        return new SearchResponse<>(0, request.getLimit(), request.getOffset(), new ArrayList<>());

                SearchResponse<FlatDataEntity> res = entityRepository.searchDataEntityByDomain(request, domainId,
                                searchableColumns, joinColumns, userDetails);
                for (FlatDataEntity flatDataEntity : res.getItems()) {
                        flatDataEntity.setBusinessEntityId(
                                        getReferenceDataAsset(flatDataEntity.getId(), ArtifactType.business_entity,
                                                        userDetails));
                }
                res.getItems().stream().forEach(
                                x -> x.setTags(tagService.getArtifactTags(x.getId(), userDetails)
                                                .stream().map(y -> y.getName()).collect(Collectors.toList())));
                return res;
        }

        public SearchResponse<FlatDataEntity> searchDataEntityByIndicator(SearchRequestWithJoin request,
                        String indicatorId,
                        UserDetails userDetails) throws LottabyteException {
                ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
                if (indicatorId == null || indicatorId.isEmpty() || !indicatorId
                                .matches("[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}")) {
                        throw new LottabyteException(Message.LBE02401,
                                                        userDetails.getLanguage(), indicatorId);
                }

                SearchResponse<FlatDataEntity> res = entityRepository.searchDataEntityByIndicator(request, indicatorId,
                                searchableColumns, joinColumns, userDetails);
                for (FlatDataEntity flatDataEntity : res.getItems()) {
                        flatDataEntity.setBusinessEntityId(
                                        getReferenceDataAsset(flatDataEntity.getId(), ArtifactType.business_entity,
                                                        userDetails));
                }
                res.getItems().stream().forEach(
                                x -> x.setTags(tagService.getArtifactTags(x.getId(), userDetails)
                                                .stream().map(y -> y.getName()).collect(Collectors.toList())));
                return res;
        }

        public SearchResponse<FlatDataEntity> searchDataEntityByBE(SearchRequestWithJoin request, String beId,
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
                                !domainRepository.hasAccessToDomain(be.getEntity().getDomainId(), userDetails))
                        return new SearchResponse<>(0, request.getLimit(), request.getOffset(), new ArrayList<>());

                SearchResponse<FlatDataEntity> res = entityRepository.searchDataEntityByBE(request, beId,
                                searchableColumns,
                                joinColumns, userDetails);
                for (FlatDataEntity item : res.getItems()) {
                        item.setSystems(systemService.getSystemsByEntityId(item.getId(), userDetails).stream()
                                        .map(y -> FlatRelation.builder()
                                                        .id(y.getId()).name(y.getName()).url("/v1/systems/" + y.getId())
                                                        .build())
                                        .collect(Collectors.toList()));
                        Set<String> domainIds = new HashSet<>();
                        item.setDomains(new ArrayList<>());
                        for (FlatRelation s : item.getSystems()) {
                                List<Domain> domains = domainService.getDomainsBySystemId(s.getId(), userDetails);
                                for (Domain d : domains) {
                                        if (!domainIds.contains(d.getId())) {
                                                item.getDomains().add(FlatRelation.builder().id(d.getId())
                                                                .name(d.getName())
                                                                .url("/v1/domains/" + d.getId()).build());
                                                domainIds.add(d.getId());
                                        }
                                }
                        }
                }
                for (FlatDataEntity flatDataEntity : res.getItems()) {
                        flatDataEntity.setBusinessEntityId(
                                        getReferenceDataAsset(flatDataEntity.getId(), ArtifactType.business_entity,
                                                        userDetails));
                }
                res.getItems().stream().forEach(
                                x -> x.setTags(tagService.getArtifactTags(x.getId(), userDetails)
                                                .stream().map(y -> y.getName()).collect(Collectors.toList())));
                return res;
        }

        public SearchResponse<FlatDataEntityAttribute> searchAttributesByEntityId(SearchRequestWithJoin request,
                        String entityId, UserDetails userDetails) throws LottabyteException {
                ServiceUtils.validateSearchRequestWithJoin(request, searchableAttrColumns, joinAttrColumns,
                                userDetails);
                if (entityId == null || entityId.isEmpty() || !entityId
                                .matches("[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}")) {
                        throw new LottabyteException(Message.LBE00101,
                                                        userDetails.getLanguage(), entityId);
                }
                if (request.getLimitSteward() != null && request.getLimitSteward() && userDetails.getStewardId() != null
                                &&
                                !entityRepository.hasAccessToEntity(entityId, userDetails))
                        return new SearchResponse<>(0, request.getLimit(), request.getOffset(), new ArrayList<>());

                SearchResponse<FlatDataEntityAttribute> res = entityRepository.searchAttributesByEntityId(request,
                                entityId,
                                searchableAttrColumns, joinAttrColumns, userDetails);

                res.getItems().stream().forEach(
                                x -> x.setTags(tagService.getArtifactTags(x.getId(), userDetails)
                                                .stream().map(y -> y.getName()).collect(Collectors.toList())));

                return res;
        }

        public SearchResponse<FlatDataEntityAttribute> searchAttributes(SearchRequestWithJoin request,
                        UserDetails userDetails) throws LottabyteException {
                ServiceUtils.validateSearchRequestWithJoin(request, searchableAttrColumns, joinAttrColumns,
                                userDetails);

                return entityRepository.searchAttributes(request, searchableAttrColumns, joinAttrColumns, userDetails);
        }

        private void updateEntitySystems(String entityId, List<String> ids, List<String> currentIds,
                        UserDetails userDetails) {
                log.info("updateEntitySystems " + (ids == null ? "null" : ids.size()) + " " + (currentIds == null ? "null" : currentIds.size()));
                ids.stream().filter(x -> (currentIds == null || !currentIds.contains(x))).collect(Collectors.toList())
                                .forEach(y -> entityRepository.addEntityToSystem(entityId, y, userDetails));
                if (currentIds != null) {
                        currentIds.stream().filter(x -> !ids.contains(x)).collect(Collectors.toList())
                                .forEach(y -> entityRepository.removeEntityFromSystem(entityId, y, userDetails));
                }
        }

        public List<EntityAttributeType> getEntityAttributeTypes(UserDetails userDetails) {
                return entityRepository.getEntityAttributeTypes(userDetails);
        }

        public GojsModelData getModel(UserDetails userDetails) {
                return entityRepository.getModel(userDetails);
        }

        public List<GojsModelNodeData> updateModel(UpdatableGojsModelData updatableGojsModelData,
                        UserDetails userDetails) {
                return entityRepository.updateModel(updatableGojsModelData, userDetails);
        }

        public SearchableDataEntity getSearchableArtifact(DataEntity dataEntity, UserDetails userDetails) {
                SearchableDataEntity sa = SearchableDataEntity.builder()
                        .id(dataEntity.getMetadata().getId())
                        .versionId(dataEntity.getMetadata().getVersionId())
                        .name(dataEntity.getMetadata().getName())
                        .description(dataEntity.getEntity().getDescription())
                        .modifiedBy(dataEntity.getMetadata().getModifiedBy())
                        .modifiedAt(dataEntity.getMetadata().getModifiedAt())
                        .artifactType(dataEntity.getMetadata().getArtifactType())
                        .effectiveStartDate(dataEntity.getMetadata().getEffectiveStartDate())
                        .effectiveEndDate(dataEntity.getMetadata().getEffectiveEndDate())
                        .tags(Helper.getEmptyListIfNull(dataEntity.getMetadata().getTags()).stream()
                                        .map(x -> x.getName()).collect(Collectors.toList()))

                        .entityFolderId(dataEntity.getEntity().getEntityFolderId())
                        .systemIds(dataEntity.getEntity().getSystemIds())
                        .roles(dataEntity.getEntity().getRoles())

                        .domains(entityRepository.getDomainIdsByEntityId(dataEntity.getId(), userDetails)).build();

                String be_id = dataEntity.getEntity().getBusinessEntityId();
                if (be_id != null && !be_id.isEmpty()) {
                        BusinessEntity be = businessEntityRepository.getById(be_id, userDetails);
                        if (be != null)
                                sa.setBusinessEntityName(be.getName());
                }

                sa.setSystemNames(
                                Helper.getEmptyListIfNull(systemRepository
                                                .getSystemsByEntityId(dataEntity.getId(), userDetails)
                                                .stream().map(pt -> pt.getName()).collect(Collectors.toList())));

                List<DataEntityAttribute> attrs = entityRepository
                                .getEntityAttributeListByEntityId(dataEntity.getMetadata().getId(), userDetails);
                if (attrs != null && !attrs.isEmpty()) {
                        sa.setAttributeNames(attrs.stream().map(dea -> dea.getName()).collect(Collectors.toList()));
                        sa.setAttributeDescriptions(
                                        attrs.stream().map(dea -> dea.getEntity().getDescription())
                                                        .collect(Collectors.toList()));
                }

                return sa;
        }

        public SearchableDataEntityFolder getFolderSearchableArtifact(DataEntityFolder dataEntityFolder,
                        UserDetails userDetails) {
                SearchableDataEntityFolder sa = SearchableDataEntityFolder.builder()
                        .id(dataEntityFolder.getMetadata().getId())
                        .versionId(dataEntityFolder.getMetadata().getVersionId())
                        .name(dataEntityFolder.getMetadata().getName())
                        .description(dataEntityFolder.getEntity().getDescription())
                        .modifiedBy(dataEntityFolder.getMetadata().getModifiedBy())
                        .modifiedAt(dataEntityFolder.getMetadata().getModifiedAt())
                        .artifactType(dataEntityFolder.getMetadata().getArtifactType())
                        .effectiveStartDate(dataEntityFolder.getMetadata().getEffectiveStartDate())
                        .effectiveEndDate(dataEntityFolder.getMetadata().getEffectiveEndDate())
                        .tags(Helper.getEmptyListIfNull(dataEntityFolder.getMetadata().getTags()).stream()
                                        .map(x -> x.getName()).collect(Collectors.toList()))

                        .children(dataEntityFolder.getEntity().getChildren())
                        .parentId(dataEntityFolder.getEntity().getParentId()).build();
                return sa;
        }

        public SearchableDataEntityAttribute getAttributeSearchableArtifact(DataEntityAttribute dataEntityAttribute,
                        UserDetails userDetails) {
                SearchableDataEntityAttribute sa = SearchableDataEntityAttribute.builder()
                        .id(dataEntityAttribute.getMetadata().getId())
                        .versionId(dataEntityAttribute.getMetadata().getVersionId())
                        .name(dataEntityAttribute.getMetadata().getName())
                        .description(dataEntityAttribute.getEntity().getDescription())
                        .modifiedBy(dataEntityAttribute.getMetadata().getModifiedBy())
                        .modifiedAt(dataEntityAttribute.getMetadata().getModifiedAt())
                        .artifactType(dataEntityAttribute.getMetadata().getArtifactType())
                        .effectiveStartDate(dataEntityAttribute.getMetadata().getEffectiveStartDate())
                        .effectiveEndDate(dataEntityAttribute.getMetadata().getEffectiveEndDate())
                        .tags(Helper.getEmptyListIfNull(dataEntityAttribute.getMetadata().getTags()).stream()
                                        .map(x -> x.getName()).collect(Collectors.toList()))

                        .entityId(dataEntityAttribute.getEntity().getEntityId())
                        .attributeType(dataEntityAttribute.getEntity().getAttributeType())
                        .enumerationId(dataEntityAttribute.getEntity().getEnumerationId())
                        .mappedSamplePropertyIds(dataEntityAttribute.getEntity().getMappedSamplePropertyIds()).build();
                return sa;
        }
}
