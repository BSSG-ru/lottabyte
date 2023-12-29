package ru.bssg.lottabyte.coreapi.service;

import org.flowable.engine.runtime.ProcessInstance;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.businessEntity.BusinessEntity;
import ru.bssg.lottabyte.core.model.entitySample.EntitySampleDQRule;
import ru.bssg.lottabyte.core.model.entitySample.UpdatableEntitySampleDQRule;
import ru.bssg.lottabyte.core.model.product.*;
import ru.bssg.lottabyte.core.model.reference.Reference;
import ru.bssg.lottabyte.core.model.reference.ReferenceEntity;
import ru.bssg.lottabyte.core.model.reference.ReferenceType;
import ru.bssg.lottabyte.core.model.reference.UpdatableReferenceEntity;
import ru.bssg.lottabyte.core.model.workflow.WorkflowType;
import ru.bssg.lottabyte.core.ui.model.SearchColumn;
import ru.bssg.lottabyte.core.ui.model.SearchColumnForJoin;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.*;
import ru.bssg.lottabyte.coreapi.util.Helper;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductService extends WorkflowableService<Product> {
    private final ProductRepository productRepository;
    private final ReferenceService referenceService;
    private final SystemService systemService;
    private final DataAssetService dataAssetService;
    private final IndicatorService indicatorService;
    private final EntityService entityService;
    private final TagService tagService;
    private final WorkflowService workflowService;
    private final ArtifactType serviceArtifactType = ArtifactType.product;
    private final DomainService domainService;
    private final ElasticsearchService elasticsearchService;
    private final EntitySampleRepository entitySampleRepository;
    private final ArtifactService artifactService;

    private final SearchColumn[] searchableColumns = {
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
            new SearchColumn("version_id", SearchColumn.ColumnType.Number),
            new SearchColumn("state", SearchColumn.ColumnType.Text),
            new SearchColumn("ancestor_draft_id", SearchColumn.ColumnType.UUID),
            new SearchColumn("published_id", SearchColumn.ColumnType.UUID),
            new SearchColumn("indicator.name", SearchColumn.ColumnType.Text),
            new SearchColumn("indicator.id", SearchColumn.ColumnType.UUID),
            new SearchColumn("domain.name", SearchColumn.ColumnType.Text),
            new SearchColumn("domain_id", SearchColumn.ColumnType.UUID),
            new SearchColumn("product_types", SearchColumn.ColumnType.Text),
            new SearchColumn("entity_attribute.name", SearchColumn.ColumnType.Text),
            new SearchColumn("workflow_state", SearchColumn.ColumnType.Text),
            new SearchColumn("tags", SearchColumn.ColumnType.Text)
    };

    private final SearchColumnForJoin[] joinColumns = {
            new SearchColumnForJoin("source_id", "reference", SearchColumn.ColumnType.UUID, "id", "target_id"),
            new SearchColumnForJoin("target_id", "reference", SearchColumn.ColumnType.UUID, "id", "source_id")
    };

    private final SearchColumn[] searchableColumnsProductType = {
            new SearchColumn("name", SearchColumn.ColumnType.Text)
    };
    private final SearchColumnForJoin[] joinColumnsProductType = {};

    private final SearchColumn[] searchableColumnsProductSupplyVariant = {
            new SearchColumn("name", SearchColumn.ColumnType.Text)
    };
    private final SearchColumnForJoin[] joinColumnsProductSupplyVariant = {};

    @Autowired
    @Lazy
    public ProductService(ProductRepository productRepository,
            EntitySampleRepository entitySampleRepository,
            ReferenceService referenceService,
            ElasticsearchService elasticsearchService,
            SystemService systemService,
            DataAssetService dataAssetService,
            IndicatorService indicatorService,
            EntityService entityService, TagService tagService,
            DomainService domainService,
            WorkflowService workflowService,
            ArtifactService artifactService) {
        super(productRepository, workflowService, tagService, ArtifactType.product, elasticsearchService);
        this.productRepository = productRepository;
        this.referenceService = referenceService;
        this.systemService = systemService;
        this.dataAssetService = dataAssetService;
        this.indicatorService = indicatorService;
        this.entityService = entityService;
        this.tagService = tagService;
        this.domainService = domainService;
        this.workflowService = workflowService;
        this.elasticsearchService = elasticsearchService;
        this.entitySampleRepository = entitySampleRepository;
        this.artifactService = artifactService;
    }

    public Product wfPublish(String draftEntityId, UserDetails userDetails) throws LottabyteException {
        Product draft = getProductById(draftEntityId, userDetails);
        String publishedId = ((WorkflowableMetadata) draft.getMetadata()).getPublishedId();

        if (publishedId == null) {
            String newPublishedId = productRepository.publishProductDraft(draftEntityId, null, userDetails);

            if (draft.getEntity() != null) {
                updateProductReference(new UpdatableProductEntity(draft.getEntity()), draft.getId(), newPublishedId,
                        userDetails);
                createProductReference(new UpdatableProductEntity(draft.getEntity()), newPublishedId, newPublishedId,
                        userDetails);
            }

            if (draft.getEntity().getDqRules() != null && !draft.getEntity().getDqRules().isEmpty())
                for (EntitySampleDQRule s : draft.getEntity().getDqRules())
                    productRepository.addDQRule(newPublishedId, s, userDetails);

            tagService.mergeTags(draftEntityId, serviceArtifactType, newPublishedId, serviceArtifactType, userDetails);
            Product product = getProductById(newPublishedId, userDetails);
            elasticsearchService.insertElasticSearchEntity(
                    Collections.singletonList(getSearchableArtifact(product, userDetails)), userDetails);
            return product;
        } else {
            productRepository.publishProductDraft(draftEntityId, publishedId, userDetails);
            Product currentPublished = getProductById(publishedId, userDetails);
            updateDQRules(publishedId, draft.getEntity().getDqRules(), currentPublished.getEntity().getDqRules(),
                    userDetails);
            if (referenceService.getReferenceBySourceId(publishedId, userDetails) != null)
                referenceService.deleteReferenceBySourceId(publishedId, userDetails);

            if (draft.getEntity() != null) {
                updateProductReference(new UpdatableProductEntity(draft.getEntity()), draft.getId(), publishedId,
                        userDetails);
                createProductReference(new UpdatableProductEntity(draft.getEntity()), publishedId, publishedId,
                        userDetails);
            }

            if (draft.getEntity().getTermLinkIds() != null && !draft.getEntity().getTermLinkIds().isEmpty()) {
                createTermLinksReference(draft.getEntity().getTermLinkIds(), publishedId, publishedId, userDetails);
            }

            tagService.mergeTags(draftEntityId, serviceArtifactType, publishedId, serviceArtifactType, userDetails);
            Product p = getProductById(publishedId, userDetails);
            elasticsearchService.updateElasticSearchEntity(
                    Collections.singletonList(getSearchableArtifact(p, userDetails)), userDetails);
            return p;
        }
    }

    public void wfApproveRemoval(String draftSystemId, UserDetails userDetails) throws LottabyteException {
        Product product = getProductById(draftSystemId, userDetails);
        if (product == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftSystemId);
        String publishedId = ((WorkflowableMetadata) product.getMetadata()).getPublishedId();
        if (publishedId == null)
            throw new LottabyteException(
                    Message.LBE03006,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftSystemId);
        productRepository.setStateById(product.getId(), ArtifactState.DRAFT_HISTORY, userDetails);
        productRepository.setStateById(publishedId, ArtifactState.REMOVED, userDetails);
        referenceService.deleteReferenceBySourceId(draftSystemId, userDetails);
        elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(publishedId), userDetails);
    }

    public Product getProductById(String productId, UserDetails userDetails) throws LottabyteException {
        Product product = productRepository.getById(productId, userDetails);
        if (product == null)
            throw new LottabyteException(Message.LBE03101,
                            userDetails.getLanguage(), productId);

        List<EntitySampleDQRule> dqRules = entitySampleRepository.getSampleDQRulesByProduct(productId, userDetails);
        product.getEntity().setDqRules(dqRules);

        List<Reference> referenceForAttr = referenceService.getAllReferenceBySourceIdAndTargetType(productId,
                String.valueOf(ArtifactType.entity_attribute), userDetails);
        List<String> attrIdList = referenceForAttr.stream().map(r -> r.getEntity().getTargetId())
                .collect(Collectors.toList());
        product.getEntity().setEntityAttributeIds(attrIdList);

        List<Reference> referenceForIndicator = referenceService.getAllReferenceBySourceIdAndTargetType(productId,
                String.valueOf(ArtifactType.indicator), userDetails);
        List<String> indicatorIdList = referenceForIndicator.stream().map(r -> r.getEntity().getTargetId())
                .collect(Collectors.toList());
        product.getEntity().setIndicatorIds(indicatorIdList);

        List<Reference> referenceForPType = referenceService.getAllReferenceBySourceIdAndTargetType(productId,
                String.valueOf(ArtifactType.product_type), userDetails);
        List<String> ptypeIdList = referenceForPType.stream().map(r -> r.getEntity().getTargetId())
                .collect(Collectors.toList());
        product.getEntity().setProductTypeIds(ptypeIdList);

        List<Reference> referenceForPSV = referenceService.getAllReferenceBySourceIdAndTargetType(productId,
                String.valueOf(ArtifactType.product_supply_variant), userDetails);
        List<String> psvIdList = referenceForPSV.stream().map(r -> r.getEntity().getTargetId())
                .collect(Collectors.toList());
        product.getEntity().setProductSupplyVariantIds(psvIdList);

        List<Reference> referenceForDataAsset = referenceService.getAllReferenceBySourceIdAndTargetType(productId,
                String.valueOf(ArtifactType.data_asset), userDetails);
        List<String> dataAssetIdList = referenceForDataAsset.stream().map(r -> r.getEntity().getTargetId())
                .collect(Collectors.toList());
        product.getEntity().setDataAssetIds(dataAssetIdList);

        product.getEntity().setTermLinkIds(
                getTermLinksById(productId, userDetails).stream().map(x -> x.getId()).collect(Collectors.toList()));

        WorkflowableMetadata md = (WorkflowableMetadata) product.getMetadata();
        if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
            md.setDraftId(productRepository.getDraftId(md.getId(), userDetails));
        product.getMetadata().setTags(tagService.getArtifactTags(productId, userDetails));
        return product;
    }

    public ProductType getProductTypeById(String productTypeId, UserDetails userDetails) throws LottabyteException {
        ProductType productType = productRepository.getProductTypeById(productTypeId, userDetails);
        if (productType == null)
            throw new LottabyteException(Message.LBE03101,
                            userDetails.getLanguage(), productTypeId);

        return productType;
    }

    public ProductSupplyVariant getProductSupplyVariantById(String productSupplyVariantId, UserDetails userDetails)
            throws LottabyteException {
        ProductSupplyVariant productSupplyVariant = productRepository
                .getProductSupplyVariantById(productSupplyVariantId, userDetails);
        if (productSupplyVariant == null)
            throw new LottabyteException(Message.LBE03101,
                            userDetails.getLanguage(), productSupplyVariantId);

        return productSupplyVariant;
    }

    public PaginatedArtifactList<Product> getAllProductsPaginated(Integer offset, Integer limit, String artifactState,
            UserDetails userDetails) throws LottabyteException {
        if (!EnumUtils.isValidEnum(ArtifactState.class, artifactState))
            throw new LottabyteException(
                    Message.LBE00067,
                            userDetails.getLanguage(),
                    artifactState);
        PaginatedArtifactList<Product> res = productRepository.getAllPaginated(offset, limit, "/v1/product/",
                ArtifactState.valueOf(artifactState), userDetails);

        for (Product product : res.getResources()) {
            List<Reference> referenceForDataAsset = referenceService.getAllReferenceBySourceIdAndTargetType(
                    product.getId(), String.valueOf(ArtifactType.entity_attribute), userDetails);
            List<String> dataAssetIdList = referenceForDataAsset.stream().map(r -> r.getEntity().getTargetId())
                    .collect(Collectors.toList());
            product.getEntity().setEntityAttributeIds(dataAssetIdList);

            List<Reference> referenceForIndicator = referenceService.getAllReferenceBySourceIdAndTargetType(
                    product.getId(), String.valueOf(ArtifactType.indicator), userDetails);
            List<String> indicatorIdList = referenceForIndicator.stream().map(r -> r.getEntity().getTargetId())
                    .collect(Collectors.toList());
            product.getEntity().setIndicatorIds(indicatorIdList);
        }

        res.getResources().forEach(
                system -> system.getMetadata().setTags(tagService.getArtifactTags(system.getId(), userDetails)));
        return res;
    }

    public PaginatedArtifactList<Product> getProductVersions(String productId, Integer offset, Integer limit,
            UserDetails userDetails) {
        if (!productRepository.existsById(productId, userDetails)) {
            PaginatedArtifactList<Product> res = new PaginatedArtifactList<>();
            res.setCount(0);
            res.setOffset(offset);
            res.setLimit(limit);
            res.setResources(new ArrayList<>());
            return res;
        }
        PaginatedArtifactList<Product> dataEntityPaginatedArtifactList = productRepository.getVersionsById(productId,
                offset, limit, "/v1/products/" + productId + "/versions", userDetails);
        for (Product product : dataEntityPaginatedArtifactList.getResources()) {
            fillProductVersionRelations(product, product.getEntity().getVersionId(), userDetails);
        }

        return dataEntityPaginatedArtifactList;
    }

    private void fillProductVersionRelations(Product product, Integer versionId, UserDetails userDetails) {
        WorkflowableMetadata md = (WorkflowableMetadata) product.getMetadata();

        if (md.getAncestorDraftId() != null) {
            product.getMetadata().setTags(tagService.getArtifactTags(md.getAncestorDraftId(), userDetails));

            List<Reference> referenceForAttr = referenceService.getAllReferenceByPublishedIdAndTypeAndVersionId(
                    product.getId(), versionId, String.valueOf(ArtifactType.entity_attribute), userDetails);
            List<String> attrIdList = referenceForAttr.stream().map(r -> r.getEntity().getTargetId())
                    .collect(Collectors.toList());
            product.getEntity().setEntityAttributeIds(attrIdList);

            List<Reference> referenceForIndicator = referenceService.getAllReferenceByPublishedIdAndTypeAndVersionId(
                    product.getId(), versionId, String.valueOf(ArtifactType.indicator), userDetails);
            List<String> indicatorIdList = referenceForIndicator.stream().map(r -> r.getEntity().getTargetId())
                    .collect(Collectors.toList());
            product.getEntity().setIndicatorIds(indicatorIdList);

            List<Reference> referenceForPType = referenceService.getAllReferenceByPublishedIdAndTypeAndVersionId(
                    product.getId(), versionId, String.valueOf(ArtifactType.product_type), userDetails);
            List<String> ptypeIdList = referenceForPType.stream().map(r -> r.getEntity().getTargetId())
                    .collect(Collectors.toList());
            product.getEntity().setProductTypeIds(ptypeIdList);

            List<Reference> referenceForPSV = referenceService.getAllReferenceByPublishedIdAndTypeAndVersionId(
                    product.getId(), versionId, String.valueOf(ArtifactType.product_supply_variant), userDetails);
            List<String> psvIdList = referenceForPSV.stream().map(r -> r.getEntity().getTargetId())
                    .collect(Collectors.toList());
            product.getEntity().setProductSupplyVariantIds(psvIdList);

            List<Reference> referenceForAsset = referenceService.getAllReferenceByPublishedIdAndTypeAndVersionId(
                    product.getId(), versionId, String.valueOf(ArtifactType.data_asset), userDetails);
            List<String> assetIdList = referenceForAsset.stream().map(r -> r.getEntity().getTargetId())
                    .collect(Collectors.toList());
            product.getEntity().setDataAssetIds(assetIdList);
        }
    }

    public Product getProductVersionById(String productId, Integer versionId, UserDetails userDetails)
            throws LottabyteException {
        Product product = productRepository.getVersionById(productId, versionId, userDetails);
        if (product == null)
            throw new LottabyteException(Message.LBE03102,
                            userDetails.getLanguage(), productId, versionId);
        fillProductVersionRelations(product, versionId, userDetails);
        return product;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Product createProduct(UpdatableProductEntity product, UserDetails userDetails) throws LottabyteException {
        if (product.getName() == null || product.getName().isEmpty())
            throw new LottabyteException(
                    Message.LBE00303, userDetails.getLanguage());
        if (!entityService.allAttributesExist(product.getEntityAttributeIds(), userDetails))
            throw new LottabyteException(Message.LBE03103,
                            userDetails.getLanguage(), StringUtils.join(product.getEntityAttributeIds(), ", "));
        if (!indicatorService.allIndicatorsExist(product.getIndicatorIds(), userDetails))
            throw new LottabyteException(Message.LBE03104,
                            userDetails.getLanguage(), StringUtils.join(product.getIndicatorIds(), ", "));

        String workflowTaskId = null;
        ProcessInstance pi = null;
        product.setId(UUID.randomUUID().toString());

        if (workflowService.isWorkflowEnabled(serviceArtifactType)
                && workflowService.getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

            pi = workflowService.startFlowableProcess(product.getId(), serviceArtifactType, ArtifactAction.CREATE,
                    userDetails);
            workflowTaskId = pi.getId();

        }

        String newEntityId = productRepository.createProduct(product, workflowTaskId, userDetails);

        createProductReference(product, newEntityId, null, userDetails);
        createTermLinksReference(product.getTermLinkIds(), newEntityId, null, userDetails);

        Product result = getProductById(newEntityId, userDetails);

        return result;
    }

    public void createTermLinksReference(List<String> termLinkIds, String newProductId, String publishedId,
            UserDetails userDetails) throws LottabyteException {
        if (termLinkIds != null && !termLinkIds.isEmpty()) {
            Integer versionId = publishedId == null ? 0
                    : referenceService.getLastVersionByPublishedId(publishedId, userDetails);
            for (String targetId : termLinkIds) {
                if (!targetId.equals(newProductId)) {
                    ReferenceEntity referenceEntity = new ReferenceEntity();
                    referenceEntity.setSourceId(newProductId);
                    referenceEntity.setSourceType(ArtifactType.product);
                    referenceEntity.setPublishedId(publishedId);
                    referenceEntity.setVersionId(versionId);
                    referenceEntity.setTargetId(targetId);
                    referenceEntity.setTargetType(ArtifactType.business_entity);
                    referenceEntity.setReferenceType(ReferenceType.PRODUCT_TO_BUSINESS_ENTITY_LINK);

                    UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                    referenceService.createReference(newReferenceEntity, userDetails);
                }
            }
        }
    }

    public void createProductReference(UpdatableProductEntity product, String newEntityId, String publishedId,
            UserDetails userDetails) throws LottabyteException {
        Integer versionId = referenceService.getLastVersionByPublishedId(publishedId, userDetails);
        if (product.getEntityAttributeIds() != null && !product.getEntityAttributeIds().isEmpty()) {
            for (String targetId : product.getEntityAttributeIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newEntityId);
                referenceEntity.setSourceType(ArtifactType.product);
                referenceEntity.setTargetId(targetId);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(ArtifactType.entity_attribute);
                referenceEntity.setReferenceType(ReferenceType.PRODUCT_TO_DATA_ENTITY_ATTRIBUTE);
                referenceEntity.setVersionId(versionId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.createReference(newReferenceEntity, userDetails);
            }
        }
        if (product.getIndicatorIds() != null && !product.getIndicatorIds().isEmpty()) {
            for (String targetId : product.getIndicatorIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newEntityId);
                referenceEntity.setSourceType(ArtifactType.product);
                referenceEntity.setTargetId(targetId);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(ArtifactType.indicator);
                referenceEntity.setReferenceType(ReferenceType.PRODUCT_TO_INDICATOR);
                referenceEntity.setVersionId(versionId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.createReference(newReferenceEntity, userDetails);
            }
        }
        if (product.getProductTypeIds() != null && !product.getProductTypeIds().isEmpty()) {
            for (String targetId : product.getProductTypeIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newEntityId);
                referenceEntity.setSourceType(ArtifactType.product);
                referenceEntity.setTargetId(targetId);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(ArtifactType.product_type);
                referenceEntity.setReferenceType(ReferenceType.PRODUCT_TO_PRODUCT_TYPE);
                referenceEntity.setVersionId(versionId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.createReference(newReferenceEntity, userDetails);
            }
        }
        if (product.getProductSupplyVariantIds() != null && !product.getProductSupplyVariantIds().isEmpty()) {
            for (String targetId : product.getProductSupplyVariantIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newEntityId);
                referenceEntity.setSourceType(ArtifactType.product);
                referenceEntity.setTargetId(targetId);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(ArtifactType.product_supply_variant);
                referenceEntity.setReferenceType(ReferenceType.PRODUCT_TO_PRODUCT_SUPPLY_VARIANT);
                referenceEntity.setVersionId(versionId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.createReference(newReferenceEntity, userDetails);
            }
        }
        if (product.getDataAssetIds() != null && !product.getDataAssetIds().isEmpty()) {
            for (String targetId : product.getDataAssetIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newEntityId);
                referenceEntity.setSourceType(ArtifactType.product);
                referenceEntity.setTargetId(targetId);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(ArtifactType.data_asset);
                referenceEntity.setReferenceType(ReferenceType.PRODUCT_TO_DATA_ASSET);
                referenceEntity.setVersionId(versionId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.createReference(newReferenceEntity, userDetails);
            }
        }
    }

    public void updateProductReference(UpdatableProductEntity product, String newEntityId, String publishedId,
            UserDetails userDetails) throws LottabyteException {
        if (product.getEntityAttributeIds() != null && !product.getEntityAttributeIds().isEmpty()) {
            for (String targetId : product.getEntityAttributeIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newEntityId);
                referenceEntity.setTargetId(targetId);
                referenceEntity.setPublishedId(publishedId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.patchReferenceBySourceIdAndTargetId(newReferenceEntity, userDetails);
            }
        }
        if (product.getIndicatorIds() != null && !product.getIndicatorIds().isEmpty()) {
            for (String targetId : product.getIndicatorIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newEntityId);
                referenceEntity.setTargetId(targetId);
                referenceEntity.setPublishedId(publishedId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.patchReferenceBySourceIdAndTargetId(newReferenceEntity, userDetails);
            }
        }
        if (product.getProductTypeIds() != null && !product.getProductTypeIds().isEmpty()) {
            for (String targetId : product.getProductTypeIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newEntityId);
                referenceEntity.setTargetId(targetId);
                referenceEntity.setPublishedId(publishedId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.patchReferenceBySourceIdAndTargetId(newReferenceEntity, userDetails);
            }
        }
        if (product.getProductSupplyVariantIds() != null && !product.getProductSupplyVariantIds().isEmpty()) {
            for (String targetId : product.getProductSupplyVariantIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newEntityId);
                referenceEntity.setTargetId(targetId);
                referenceEntity.setPublishedId(publishedId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.patchReferenceBySourceIdAndTargetId(newReferenceEntity, userDetails);
            }
        }
        if (product.getDataAssetIds() != null && !product.getDataAssetIds().isEmpty()) {
            for (String targetId : product.getDataAssetIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(newEntityId);
                referenceEntity.setTargetId(targetId);
                referenceEntity.setPublishedId(publishedId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.patchReferenceBySourceIdAndTargetId(newReferenceEntity, userDetails);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Product updateProduct(String productId, UpdatableProductEntity productEntity, UserDetails userDetails)
            throws LottabyteException {
        if (!productRepository.existsById(productId, userDetails))
            throw new LottabyteException(Message.LBE03101,
                            userDetails.getLanguage(), productId);

        if (productEntity.getEntityAttributeIds() != null && !productEntity.getEntityAttributeIds().isEmpty()
                && !entityService.allAttributesExist(productEntity.getEntityAttributeIds(), userDetails))
            throw new LottabyteException(Message.LBE03103,
                            userDetails.getLanguage(),
                            StringUtils.join(productEntity.getEntityAttributeIds(), ", "));
        if (productEntity.getIndicatorIds() != null && !productEntity.getIndicatorIds().isEmpty()
                && !indicatorService.allIndicatorsExist(productEntity.getIndicatorIds(), userDetails))
            throw new LottabyteException(Message.LBE03104,
                            userDetails.getLanguage(), StringUtils.join(productEntity.getIndicatorIds(), ", "));
        if (productEntity.getDataAssetIds() != null && !productEntity.getDataAssetIds().isEmpty()
                && !dataAssetService.allDataAssetsExist(productEntity.getDataAssetIds(), userDetails))
            throw new LottabyteException(Message.LBE03105,
                            userDetails.getLanguage(), StringUtils.join(productEntity.getDataAssetIds(), ", "));

        Product current = getProductById(productId, userDetails);
        Integer versionId = current.getEntity().getVersionId();
        versionId++;
        String draftId = null;
        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            draftId = productRepository.getDraftId(productId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE00326,
                                userDetails.getLanguage(),
                        draftId);
        }
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

            productRepository.createProductDraft(current.getId(), draftId, workflowTaskId, userDetails);

            if (productEntity.getDqRules() != null && !productEntity.getDqRules().isEmpty()) {
                for (EntitySampleDQRule s : productEntity.getDqRules()) {
                    productRepository.addDQRule(draftId, s, userDetails);
                }
            } else if (current.getEntity().getDqRules() != null && !current.getEntity().getDqRules().isEmpty()) {
                for (EntitySampleDQRule s : current.getEntity().getDqRules()) {
                    productRepository.addDQRule(draftId, s, userDetails);
                }
            }

            if (productEntity.getEntityAttributeIds() != null) {
                createReferenceForAttributes(draftId, productEntity.getEntityAttributeIds(), productId, versionId,
                        userDetails);
            } else {
                if (current.getEntity().getEntityAttributeIds() != null
                        && !current.getEntity().getEntityAttributeIds().isEmpty()) {
                    createReferenceForAttributes(draftId, current.getEntity().getEntityAttributeIds(), productId,
                            versionId, userDetails);
                }
            }

            if (productEntity.getIndicatorIds() != null) {
                createReferenceForIndicators(draftId, productEntity.getIndicatorIds(), productId, versionId,
                        userDetails);
            } else {
                if (current.getEntity().getIndicatorIds() != null && !current.getEntity().getIndicatorIds().isEmpty()) {
                    createReferenceForIndicators(draftId, current.getEntity().getIndicatorIds(), productId, versionId,
                            userDetails);
                }
            }

            if (productEntity.getProductTypeIds() != null) {
                createReference(draftId, productEntity.getProductTypeIds(), ArtifactType.product_type,
                        ReferenceType.PRODUCT_TO_PRODUCT_TYPE, productId, versionId, userDetails);
            } else {
                if (current.getEntity().getProductTypeIds() != null
                        && !current.getEntity().getProductTypeIds().isEmpty()) {
                    createReference(draftId, current.getEntity().getProductTypeIds(), ArtifactType.product_type,
                            ReferenceType.PRODUCT_TO_PRODUCT_TYPE, productId, versionId, userDetails);
                }
            }

            if (productEntity.getProductSupplyVariantIds() != null) {
                createReference(draftId, productEntity.getProductSupplyVariantIds(),
                        ArtifactType.product_supply_variant, ReferenceType.PRODUCT_TO_PRODUCT_SUPPLY_VARIANT, productId,
                        versionId, userDetails);
            } else {
                if (current.getEntity().getProductSupplyVariantIds() != null
                        && !current.getEntity().getProductSupplyVariantIds().isEmpty()) {
                    createReference(draftId, current.getEntity().getProductSupplyVariantIds(),
                            ArtifactType.product_supply_variant, ReferenceType.PRODUCT_TO_PRODUCT_SUPPLY_VARIANT,
                            productId, versionId, userDetails);
                }
            }

            if (productEntity.getDataAssetIds() != null) {
                createReference(draftId, productEntity.getDataAssetIds(), ArtifactType.data_asset,
                        ReferenceType.PRODUCT_TO_DATA_ASSET, productId, versionId, userDetails);
            } else {
                if (current.getEntity().getDataAssetIds() != null && !current.getEntity().getDataAssetIds().isEmpty()) {
                    createReference(draftId, current.getEntity().getDataAssetIds(), ArtifactType.data_asset,
                            ReferenceType.PRODUCT_TO_DATA_ASSET, productId, versionId, userDetails);
                }
            }

            if (productEntity.getTermLinkIds() != null) {
                createTermLinksReference(productEntity.getTermLinkIds(), draftId, productId, userDetails);
            } else {
                if (current.getEntity().getTermLinkIds() != null) {
                    createTermLinksReference(current.getEntity().getTermLinkIds(), draftId, productId,
                            userDetails);
                }
            }

            tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

        } else {
            draftId = productId;
            updateDQRules(productId, productEntity.getDqRules(), current.getEntity().getDqRules(), userDetails);
            if (productEntity.getEntityAttributeIds() != null) {
                if (current.getEntity().getEntityAttributeIds() != null
                        && !current.getEntity().getEntityAttributeIds().isEmpty()) {
                    for (String dataEntityAttributeId : current.getEntity().getEntityAttributeIds()) {
                        referenceService.deleteByReferenceSourceIdAndTargetId(draftId, dataEntityAttributeId,
                                userDetails);
                    }
                }
                createReferenceForAttributes(draftId, productEntity.getEntityAttributeIds(), productId, versionId,
                        userDetails);
            }

            if (productEntity.getIndicatorIds() != null) {
                if (current.getEntity().getIndicatorIds() != null && !current.getEntity().getIndicatorIds().isEmpty()) {
                    for (String indicatorId : current.getEntity().getIndicatorIds()) {
                        referenceService.deleteByReferenceSourceIdAndTargetId(draftId, indicatorId, userDetails);
                    }
                }
                createReferenceForIndicators(draftId, productEntity.getIndicatorIds(), productId, versionId,
                        userDetails);
            }

            if (productEntity.getProductTypeIds() != null) {
                if (current.getEntity().getProductTypeIds() != null
                        && !current.getEntity().getProductTypeIds().isEmpty()) {
                    for (String id : current.getEntity().getProductTypeIds()) {
                        referenceService.deleteByReferenceSourceIdAndTargetId(draftId, id, userDetails);
                    }
                }
                createReference(draftId, productEntity.getProductTypeIds(), ArtifactType.product_type,
                        ReferenceType.PRODUCT_TO_PRODUCT_TYPE, productId, versionId, userDetails);
            }

            if (productEntity.getProductSupplyVariantIds() != null) {
                if (current.getEntity().getProductSupplyVariantIds() != null
                        && !current.getEntity().getProductSupplyVariantIds().isEmpty()) {
                    for (String id : current.getEntity().getProductSupplyVariantIds()) {
                        referenceService.deleteByReferenceSourceIdAndTargetId(draftId, id, userDetails);
                    }
                }
                createReference(draftId, productEntity.getProductSupplyVariantIds(),
                        ArtifactType.product_supply_variant, ReferenceType.PRODUCT_TO_PRODUCT_SUPPLY_VARIANT, productId,
                        versionId, userDetails);
            }

            if (productEntity.getDataAssetIds() != null) {
                if (current.getEntity().getDataAssetIds() != null && !current.getEntity().getDataAssetIds().isEmpty()) {
                    for (String id : current.getEntity().getDataAssetIds()) {
                        referenceService.deleteByReferenceSourceIdAndTargetId(draftId, id, userDetails);
                    }
                }
                createReference(draftId, productEntity.getDataAssetIds(), ArtifactType.data_asset,
                        ReferenceType.PRODUCT_TO_DATA_ASSET, productId, versionId, userDetails);
            }

            if (current.getEntity().getTermLinkIds() != null && !current.getEntity().getTermLinkIds().isEmpty()) {
                if (productEntity.getTermLinkIds() != null) {
                    for (String id : current.getEntity().getTermLinkIds()) {
                        referenceService.deleteByReferenceSourceIdAndTargetId(draftId, id,
                                userDetails);
                    }
                    createTermLinksReference(productEntity.getTermLinkIds(), draftId, productId, userDetails);
                }
            } else {
                if (productEntity.getTermLinkIds() != null && !productEntity.getTermLinkIds().isEmpty()) {
                    createTermLinksReference(productEntity.getTermLinkIds(), draftId, productId, userDetails);
                }
            }
        }

        productRepository.updateProduct(draftId, productEntity, userDetails);
        return

        getProductById(draftId, userDetails);
    }

    private void updateDQRules(String productId, List<EntitySampleDQRule> ids, List<EntitySampleDQRule> currentIds,
            UserDetails userDetails) {
        if (currentIds != null && ids != null) {
            ids.stream().filter(x -> !EntitySampleService.containsDQRule(currentIds, x)).collect(Collectors.toList())
                    .forEach(y -> productRepository.addDQRule(productId, y, userDetails));
            currentIds.stream().filter(x -> !EntitySampleService.containsDQRule(ids, x)).collect(Collectors.toList())
                    .forEach(y -> productRepository.removeDQRule(y.getId(), userDetails));
        }
    }

    private void createReferenceForAttributes(String sourceId, List<String> dataEntityAttributeIds, String publishedId,
            Integer versionId, UserDetails userDetails) throws LottabyteException {
        if (dataEntityAttributeIds != null && !dataEntityAttributeIds.isEmpty()) {
            for (String targetId : dataEntityAttributeIds) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(sourceId);
                referenceEntity.setSourceType(ArtifactType.product);
                referenceEntity.setTargetId(targetId);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(ArtifactType.entity_attribute);
                referenceEntity.setReferenceType(ReferenceType.PRODUCT_TO_DATA_ENTITY_ATTRIBUTE);
                referenceEntity.setVersionId(versionId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.createReference(newReferenceEntity, userDetails);
            }
        }
    }

    private void createReferenceForIndicators(String sourceId, List<String> indicatorIds, String publishedId,
            Integer versionId, UserDetails userDetails) throws LottabyteException {
        if (indicatorIds != null && !indicatorIds.isEmpty()) {
            for (String targetId : indicatorIds) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(sourceId);
                referenceEntity.setSourceType(ArtifactType.product);
                referenceEntity.setTargetId(targetId);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(ArtifactType.indicator);
                referenceEntity.setReferenceType(ReferenceType.PRODUCT_TO_INDICATOR);
                referenceEntity.setVersionId(versionId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.createReference(newReferenceEntity, userDetails);
            }
        }
    }

    private void createReference(String sourceId, List<String> artifactIds, ArtifactType artifactType,
            ReferenceType referenceType, String publishedId, Integer versionId, UserDetails userDetails)
            throws LottabyteException {
        if (artifactIds != null && !artifactIds.isEmpty()) {
            for (String targetId : artifactIds) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(sourceId);
                referenceEntity.setSourceType(ArtifactType.product);
                referenceEntity.setTargetId(targetId);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(artifactType);
                referenceEntity.setReferenceType(referenceType);
                referenceEntity.setVersionId(versionId);

                UpdatableReferenceEntity newReferenceEntity = new UpdatableReferenceEntity(referenceEntity);
                referenceService.createReference(newReferenceEntity, userDetails);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Product deleteProductById(String productId, UserDetails userDetails) throws LottabyteException {
        if (!productRepository.existsById(productId, userDetails))
            throw new LottabyteException(Message.LBE03101,
                            userDetails.getLanguage(), productId);

        Product current = getProductById(productId, userDetails);
        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            String draftId = productRepository.getDraftId(productId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE00505,
                                userDetails.getLanguage(),
                        draftId);

            ProcessInstance pi = null;
            String workflowTaskId = null;

            draftId = UUID.randomUUID().toString();
            pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.REMOVE, userDetails);
            workflowTaskId = pi.getId();

            productRepository.createDraftFromPublished(current.getId(), draftId, workflowTaskId, userDetails);

            createTermLinksReference(current.getEntity().getTermLinkIds(), draftId, productId, userDetails);

            return getProductById(draftId, userDetails);
        } else {
            referenceService.deleteReferenceBySourceId(productId, userDetails);
            tagService.deleteAllTagsByArtifactId(productId, userDetails);
            productRepository.deleteById(productId, userDetails);
            return null;
        }
    }

    @Override
    public Product getById(String id, UserDetails userDetails) throws LottabyteException {
        return getProductById(id, userDetails);
    }

    public List<ProductType> getProductTypesByProductId(String productId, UserDetails userDetails) {
        return productRepository.getProductTypesByProductId(productId, userDetails);
    }

    public List<BusinessEntity> getTermLinksById(String id, UserDetails userDetails) {
        return productRepository.getTermLinksById(id, userDetails);
    }

    public SearchResponse<FlatProduct> searchProducts(SearchRequestWithJoin request, UserDetails userDetails)
            throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        SearchResponse<FlatProduct> res = productRepository.searchProducts(request, searchableColumns, joinColumns,
                userDetails);
        res.getItems().stream().forEach(
                x -> x.setTags(tagService.getArtifactTags(x.getId(), userDetails)
                        .stream().map(y -> y.getName()).collect(Collectors.toList())));
        for (FlatProduct item : res.getItems()) {
            List<Reference> referenceForDataAsset = referenceService.getAllReferenceBySourceIdAndTargetType(
                    item.getId(), String.valueOf(ArtifactType.entity_attribute), userDetails);
            List<String> dataAssetIdList = referenceForDataAsset.stream().map(r -> r.getEntity().getTargetId())
                    .collect(Collectors.toList());

            List<Reference> referenceForIndicator = referenceService.getAllReferenceBySourceIdAndTargetType(
                    item.getId(), String.valueOf(ArtifactType.indicator), userDetails);
            List<String> indicatorIdList = referenceForIndicator.stream().map(r -> r.getEntity().getTargetId())
                    .collect(Collectors.toList());

            item.setEntityAttributeIds(dataAssetIdList);
            item.setIndicatorIds(indicatorIdList);
            item.setProductTypes(
                    getProductTypesByProductId(item.getId(), userDetails).stream().map(y -> FlatRelation.builder()
                            .id(y.getId()).name(y.getName()).build()).collect(Collectors.toList()));
        }
        return res;
    }

    public SearchResponse<FlatProductType> searchProductTypes(SearchRequestWithJoin request, UserDetails userDetails)
            throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumnsProductType, joinColumnsProductType,
                userDetails);
        SearchResponse<FlatProductType> res = productRepository.searchProductTypes(request,
                searchableColumnsProductType, joinColumnsProductType, userDetails);
        return res;
    }

    public SearchResponse<FlatProductSupplyVariant> searchProductSupplyVariants(SearchRequestWithJoin request,
            UserDetails userDetails) throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumnsProductSupplyVariant,
                joinColumnsProductSupplyVariant, userDetails);
        SearchResponse<FlatProductSupplyVariant> res = productRepository.searchProductSupplyVariants(request,
                searchableColumnsProductSupplyVariant, joinColumnsProductSupplyVariant, userDetails);
        return res;
    }

    @Override
    public String createDraft(String publishedId, WorkflowState workflowState, WorkflowType workflowType,
            UserDetails userDetails) throws LottabyteException {
        Product current = getById(publishedId, userDetails);

        ProcessInstance pi = null;
        String workflowTaskId = null;
        String draftId = UUID.randomUUID().toString();
        if (workflowService.isWorkflowEnabled(serviceArtifactType) && workflowService
                .getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

            pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.UPDATE,
                    userDetails);
            workflowTaskId = pi.getId();

        }
        productRepository.createDraftFromPublished(publishedId, draftId, workflowTaskId, userDetails);

        if (current.getEntity().getProductTypeIds() != null && !current.getEntity().getProductTypeIds().isEmpty()) {
            for (String s : current.getEntity().getProductTypeIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(draftId);
                referenceEntity.setSourceType(ArtifactType.product);
                referenceEntity.setTargetId(s);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(ArtifactType.product_type);
                referenceEntity.setReferenceType(ReferenceType.PRODUCT_TO_PRODUCT_TYPE);
                referenceEntity.setVersionId(0);

                referenceService.createReference(new UpdatableReferenceEntity(referenceEntity), userDetails);
            }
        }
        if (current.getEntity().getProductSupplyVariantIds() != null
                && !current.getEntity().getProductSupplyVariantIds().isEmpty()) {
            for (String s : current.getEntity().getProductSupplyVariantIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(draftId);
                referenceEntity.setSourceType(ArtifactType.product);
                referenceEntity.setTargetId(s);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(ArtifactType.product_supply_variant);
                referenceEntity.setReferenceType(ReferenceType.PRODUCT_TO_PRODUCT_SUPPLY_VARIANT);
                referenceEntity.setVersionId(0);

                referenceService.createReference(new UpdatableReferenceEntity(referenceEntity), userDetails);
            }
        }
        if (current.getEntity().getDataAssetIds() != null && !current.getEntity().getDataAssetIds().isEmpty()) {
            for (String s : current.getEntity().getDataAssetIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(draftId);
                referenceEntity.setSourceType(ArtifactType.product);
                referenceEntity.setTargetId(s);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(ArtifactType.data_asset);
                referenceEntity.setReferenceType(ReferenceType.PRODUCT_TO_DATA_ASSET);
                referenceEntity.setVersionId(0);

                referenceService.createReference(new UpdatableReferenceEntity(referenceEntity), userDetails);
            }
        }
        if (current.getEntity().getIndicatorIds() != null && !current.getEntity().getIndicatorIds().isEmpty()) {
            for (String s : current.getEntity().getIndicatorIds()) {
                ReferenceEntity referenceEntity = new ReferenceEntity();
                referenceEntity.setSourceId(draftId);
                referenceEntity.setSourceType(ArtifactType.product);
                referenceEntity.setTargetId(s);
                referenceEntity.setPublishedId(publishedId);
                referenceEntity.setTargetType(ArtifactType.indicator);
                referenceEntity.setReferenceType(ReferenceType.PRODUCT_TO_INDICATOR);
                referenceEntity.setVersionId(0);

                referenceService.createReference(new UpdatableReferenceEntity(referenceEntity), userDetails);
            }
        }

        if (current.getEntity().getEntityAttributeIds() != null
                && !current.getEntity().getEntityAttributeIds().isEmpty()) {
            createReferenceForAttributes(draftId, current.getEntity().getEntityAttributeIds(), publishedId, 0,
                    userDetails);
        }

        return draftId;
    }

    public EntitySampleDQRule createDQRule(String productId,
            UpdatableEntitySampleDQRule entitySampleDQRule, UserDetails userDetails)
            throws LottabyteException {

        EntitySampleDQRule sampleDQRule = productRepository.createDQRule(productId,
                entitySampleDQRule, userDetails);
        return entitySampleRepository.getSampleDQRule(sampleDQRule.getId(), userDetails);
    }

    public List<EntitySampleDQRule> getDQRules(String productId,
            UserDetails userDetails) throws LottabyteException {

        return entitySampleRepository.getSampleDQRulesByProduct(productId,
                userDetails);
    }

    public SearchableProduct getSearchableArtifact(Product product, UserDetails userDetails) {
        SearchableProduct sa = SearchableProduct.builder()
            .id(product.getMetadata().getId())
            .versionId(product.getMetadata().getVersionId())
            .name(product.getMetadata().getName())
            .description(product.getEntity().getDescription())
            .modifiedBy(product.getMetadata().getModifiedBy())
            .modifiedAt(product.getMetadata().getModifiedAt())
            .artifactType(product.getMetadata().getArtifactType())
            .effectiveStartDate(product.getMetadata().getEffectiveStartDate())
            .effectiveEndDate(product.getMetadata().getEffectiveEndDate())
            .tags(Helper.getEmptyListIfNull(product.getMetadata().getTags()).stream()
                    .map(x -> x.getName()).collect(Collectors.toList()))

            .indicatorIds(product.getEntity().getIndicatorIds())
            .indicatorNames(
                    Helper.getEmptyListIfNull(productRepository.getIndicatorsByProductId(product.getId(), userDetails)
                            .stream().map(ind -> ind.getName()).collect(Collectors.toList())))
            .entityAttributeIds(product.getEntity().getEntityAttributeIds())
            .domainId(product.getEntity().getDomainId())
            .problem(product.getEntity().getProblem())
            .consumer(product.getEntity().getConsumer())
            .value(product.getEntity().getValue())
            .financeSource(product.getEntity().getFinanceSource())
            .productTypeIds(Helper.getEmptyListIfNull(product.getEntity().getProductTypeIds()))
            .productTypeNames(
                    Helper.getEmptyListIfNull(productRepository.getProductTypesByProductId(product.getId(), userDetails)
                            .stream().map(pt -> pt.getName()).collect(Collectors.toList())))
            .productSupplyVariantIds(Helper.getEmptyListIfNull(product.getEntity().getProductSupplyVariantIds()))
            .productSupplyVariantNames(Helper
                    .getEmptyListIfNull(productRepository.getProductSupplyVariantsByProductId(product.getId(), userDetails)
                            .stream().map(psv -> psv.getName()).collect(Collectors.toList())))

            .domains(Collections.singletonList(product.getEntity().getDomainId()))
            .link(product.getEntity().getLink())
            .limits(product.getEntity().getLimits())
            .limits_internal(product.getEntity().getLimits_internal())
            .roles(product.getEntity().getRoles()).build();
        return sa;
    }
}
