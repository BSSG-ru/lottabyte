package ru.bssg.lottabyte.coreapi.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.FlatRelation;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.dataasset.DataAsset;
import ru.bssg.lottabyte.core.model.domain.*;
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
public class DomainService extends WorkflowableService<Domain> {
    private final DomainRepository domainRepository;
    private final TagService tagService;
    private final DataAssetRepository dataAssetRepository;
    private final ElasticsearchService elasticsearchService;
    private final SystemRepository systemRepository;
    private final ProductRepository productRepository;
    private final BusinessEntityRepository businessEntityRepository;
    private final IndicatorRepository indicatorRepository;
    private final UserRepository userRepository;
    private final StewardService stewardService;
    private final WorkflowService workflowService;
    private final ArtifactService artifactService;

    private final ArtifactType serviceArtifactType = ArtifactType.domain;

    private final SearchColumn[] searchableColumns = {
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
            new SearchColumn("stewards", SearchColumn.ColumnType.Array),
            new SearchColumn("tags", SearchColumn.ColumnType.Text),
            new SearchColumn("workflow_state", SearchColumn.ColumnType.Text)
    };

    private final SearchColumnForJoin[] joinColumns = {};

    @Autowired
    @Lazy
    public DomainService(DomainRepository domainRepository, TagService tagService,
            ElasticsearchService elasticsearchService, DataAssetRepository dataAssetRepository,
            SystemRepository systemRepository, WorkflowService workflowService,
            StewardService stewardService, ProductRepository productRepository,
            BusinessEntityRepository businessEntityRepository,
            IndicatorRepository indicatorRepository,
            UserRepository userRepository,
            ArtifactService artifactService) {
        super(domainRepository, workflowService, tagService, ArtifactType.domain, elasticsearchService);
        this.domainRepository = domainRepository;
        this.tagService = tagService;
        this.elasticsearchService = elasticsearchService;
        this.dataAssetRepository = dataAssetRepository;
        this.systemRepository = systemRepository;
        this.stewardService = stewardService;
        this.workflowService = workflowService;
        this.productRepository = productRepository;
        this.businessEntityRepository = businessEntityRepository;
        this.indicatorRepository = indicatorRepository;
        this.userRepository = userRepository;
        this.artifactService = artifactService;
    }

    // Wf interface

    public boolean existsInState(String artifactId, ArtifactState artifactState, UserDetails userDetails)
            throws LottabyteException {
        Domain domain = getDomainById(artifactId, userDetails);
        WorkflowableMetadata md = (WorkflowableMetadata) domain.getMetadata();
        if (!artifactState.equals(md.getState()))
            return false;
        return true;
    }

    public String getDraftArtifactId(String publishedId, UserDetails userDetails) {
        return domainRepository.getDraftId(publishedId, userDetails);
    }

    /*
     * public Domain createDraft(String publishedId, WorkflowState workflowState,
     * UserDetails userDetails) throws LottabyteException {
     * Domain current = getDomainById(publishedId, userDetails);
     * String draftId = createDraftDomain(current, workflowState, userDetails);
     * return getDomainById(draftId, userDetails);
     * }
     */

    public void wfCancel(String draftDomainId, UserDetails userDetails) throws LottabyteException {
        Domain current = domainRepository.getById(draftDomainId, userDetails);
        if (current == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftDomainId);
        if (!ArtifactState.DRAFT.equals(((WorkflowableMetadata) current.getMetadata()).getState()))
            throw new LottabyteException(
                    Message.LBE03003, userDetails.getLanguage());
        domainRepository.setStateById(draftDomainId, ArtifactState.DRAFT_HISTORY, userDetails);
    }

    public void wfApproveRemoval(String draftDomainId, UserDetails userDetails) throws LottabyteException {
        Domain current = domainRepository.getById(draftDomainId, userDetails);
        if (current == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftDomainId);
        String publishedId = ((WorkflowableMetadata) current.getMetadata()).getPublishedId();
        if (publishedId == null)
            throw new LottabyteException(
                    Message.LBE03006,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftDomainId);
        domainRepository.setStateById(current.getId(), ArtifactState.DRAFT_HISTORY, userDetails);
        domainRepository.setStateById(publishedId, ArtifactState.REMOVED, userDetails);
        elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(publishedId), userDetails);
    }

    public Domain wfPublish(String draftDomainId, UserDetails userDetails) throws LottabyteException {
        Domain draft = domainRepository.getById(draftDomainId, userDetails);
        String publishedId = ((WorkflowableMetadata) draft.getMetadata()).getPublishedId();
        if (draft == null)
            throw new LottabyteException(
                    Message.LBE03004,
                            userDetails.getLanguage(),
                    serviceArtifactType, draftDomainId);
        if (domainRepository.domainNameExists(draft.getEntity().getName(), publishedId, userDetails))
            throw new LottabyteException(
                    Message.LBE00108,
                            userDetails.getLanguage(),
                    draft.getEntity().getName());

        if (publishedId == null) {
            String newPublishedId = domainRepository.publishDomainDraft(draftDomainId, null, userDetails);
            addDomainLinks(newPublishedId, draft.getEntity(), userDetails);
            tagService.mergeTags(draftDomainId, serviceArtifactType, newPublishedId, serviceArtifactType, userDetails);
            Domain d = getDomainById(newPublishedId, userDetails);
            elasticsearchService.insertElasticSearchEntity(
                    Collections.singletonList(getSearchableArtifact(d, userDetails)), userDetails);
            return d;
        } else {
            domainRepository.publishDomainDraft(draftDomainId, publishedId, userDetails);
            Domain currentPublished = domainRepository.getById(publishedId, userDetails);
            updateDomainSystems(publishedId, draft.getEntity().getSystemIds(),
                    currentPublished.getEntity().getSystemIds(), userDetails);
            updateDomainStewards(publishedId, draft.getEntity().getStewards(),
                    currentPublished.getEntity().getStewards(), userDetails);
            tagService.mergeTags(draftDomainId, serviceArtifactType, publishedId, serviceArtifactType, userDetails);
            Domain d = getDomainById(publishedId, userDetails);
            elasticsearchService.insertElasticSearchEntity(
                    Collections.singletonList(getSearchableArtifact(d, userDetails)), userDetails);
            return d;
        }
    }

    // Domain

    public Domain getById(String id, UserDetails userDetails) throws LottabyteException {
        return getDomainById(id, userDetails);
    }

    public Domain getDomainById(String domainId, UserDetails userDetails) throws LottabyteException {
        Domain domain = domainRepository.getById(domainId, userDetails);
        if (domain == null)
            throw new LottabyteException(Message.LBE00101,
                            userDetails.getLanguage(), domainId);
        WorkflowableMetadata md = (WorkflowableMetadata) domain.getMetadata();
        if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
            md.setDraftId(domainRepository.getDraftId(md.getId(), userDetails));
        domain.getMetadata().setTags(tagService.getArtifactTags(domainId, userDetails));
        return domain;
    }

    public Domain getDomainByIdAndState(String domainId, UserDetails userDetails) throws LottabyteException {
        Domain domain = domainRepository.getDomainByIdAndState(domainId, userDetails);
        if (domain == null)
            throw new LottabyteException(Message.LBE00101,
                            userDetails.getLanguage(), domainId);
        WorkflowableMetadata md = (WorkflowableMetadata) domain.getMetadata();
        if (md.getState() != null && md.getState().equals(ArtifactState.PUBLISHED))
            md.setDraftId(domainRepository.getDraftId(md.getId(), userDetails));
        domain.getMetadata().setTags(tagService.getArtifactTags(domainId, userDetails));
        return domain;
    }

    public Domain getDomainVersionById(String domainId, Integer versionId, UserDetails userDetails)
            throws LottabyteException {
        Domain domain = domainRepository.getVersionById(domainId, versionId, userDetails);
        if (domain == null)
            throw new LottabyteException(Message.LBE00103,
                            userDetails.getLanguage(), domainId, versionId);
        fillDomainRelations(domain, userDetails);
        return domain;
    }

    public boolean hasAccessToDomain(String domainId, UserDetails userDetails) {
        return domainRepository.hasAccessToDomain(domainId, userDetails);
    }

    // Delete domain
    // If domain state is PUBLISHED - create DRAFT with MARKED_FOR_REMOVAL workflow
    // state
    // If domain state is DRAFT - remove domain and corresponding workflow task
    public Domain deleteDomainById(String domainId, UserDetails userDetails) throws LottabyteException {
        if (!domainRepository.domainExists(domainId,
                new ArtifactState[] { ArtifactState.PUBLISHED, ArtifactState.DRAFT }, userDetails))
            throw new LottabyteException(
                    Message.LBE00001,
                            userDetails.getLanguage(),
                    domainId);
        if (userDetails.getStewardId() != null && !domainRepository.hasAccessToDomain(domainId, userDetails))
            throw new LottabyteException(
                    Message.LBE00113,
                            userDetails.getLanguage(),
                    domainId);

        Domain current = domainRepository.getById(domainId, userDetails);
        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            if (domainRepository.existsSystemsInDomain(domainId, userDetails))
                throw new LottabyteException(
                        Message.LBE00112, userDetails.getLanguage());
            if (domainRepository.domainHasStewards(domainId, userDetails))
                throw new LottabyteException(
                        Message.LBE00107,
                                userDetails.getLanguage(),
                        domainId);
            if (dataAssetRepository.existsDataAssetWithDomain(domainId, userDetails))
                throw new LottabyteException(
                        Message.LBE00111,
                                userDetails.getLanguage(),
                        domainId);
            if (productRepository.existsProductWithDomain(domainId, userDetails))
                throw new LottabyteException(
                        Message.LBE00120,
                                userDetails.getLanguage(),
                        domainId);
            if (businessEntityRepository.existsBusinessEntityWithDomain(domainId, userDetails))
                throw new LottabyteException(
                        Message.LBE00121,
                                userDetails.getLanguage(),
                        domainId);
            if (indicatorRepository.existsIndicatorWithDomain(domainId, userDetails))
                throw new LottabyteException(
                        Message.LBE00122,
                                userDetails.getLanguage(),
                        domainId);
            if (userRepository.existsUserWithDomain(domainId, userDetails))
                throw new LottabyteException(
                        Message.LBE00123,
                                userDetails.getLanguage(),
                        domainId);

            String draftId = domainRepository.getDraftId(domainId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE00119,
                                userDetails.getLanguage(),
                        draftId);
            draftId = createDraftDomain(current, WorkflowState.MARKED_FOR_REMOVAL, userDetails);
            return domainRepository.getById(draftId, userDetails);
        } else {
            domainRepository.deleteById(domainId, userDetails);
            removeDomainLinks(current, userDetails);
            tagService.deleteAllTagsByArtifactId(domainId, userDetails);
            return null;
        }
        // elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(domainId),
        // userDetails);
    }

    private void removeDomainLinks(Domain domain, UserDetails userDetails) throws LottabyteException {
        if (domain.getEntity().getSystemIds() != null && !domain.getEntity().getSystemIds().isEmpty())
            for (String s : domain.getEntity().getSystemIds())
                systemRepository.removeSystemFromDomain(s, domain.getId(), userDetails);
        if (domain.getEntity().getStewards() != null && !domain.getEntity().getStewards().isEmpty())
            for (String s : domain.getEntity().getStewards())
                stewardService.addStewardToDomain(s, domain.getId(), false, userDetails);
    }

    private void addDomainLinks(String domainId, DomainEntity domain, UserDetails userDetails)
            throws LottabyteException {
        if (domain.getSystemIds() != null && !domain.getSystemIds().isEmpty()) {
            domain.getSystemIds().stream().forEach(x -> domainRepository.addSystemToDomain(x, domainId, userDetails));
        }
        if (domain.getStewards() != null && !domain.getStewards().isEmpty()) {
            for (String s : domain.getStewards())
                stewardService.addStewardToDomain(s, domainId, false, userDetails);
        }
    }

    @Transactional
    public Domain createDomain(UpdatableDomainEntity domainEntity, UserDetails userDetails) throws LottabyteException {
        if (domainEntity.getName() == null || domainEntity.getName().isEmpty())
            throw new LottabyteException(
                    Message.LBE00104, userDetails.getLanguage());
        /*
         * if (domainRepository.domainNameExists(domainEntity.getName(), userDetails))
         * throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.LBE00108,
         * domainEntity.getName());
         */
        validateSystemIds(null, domainEntity, null, userDetails);
        validateStewardsIds(null, domainEntity, userDetails);

        String workflowTaskId = null;
        ProcessInstance pi = null;

        domainEntity.setId(UUID.randomUUID().toString());
        if (workflowService.isWorkflowEnabled(serviceArtifactType)
                && workflowService.getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

            pi = workflowService.startFlowableProcess(domainEntity.getId(), serviceArtifactType, ArtifactAction.CREATE,
                    userDetails);
            workflowTaskId = pi.getId();

        }
        String newDomainId = domainRepository.createDomain(domainEntity, workflowTaskId, userDetails);
        addDomainLinks(newDomainId, domainEntity, userDetails);
        Domain domain = domainRepository.getById(newDomainId, userDetails);

        return domain;
    }

    @Override
    public String createDraft(String publishedId, WorkflowState workflowState, WorkflowType workflowType,
            UserDetails userDetails) throws LottabyteException {
        Domain current = getDomainById(publishedId, userDetails);

        ProcessInstance pi = null;
        String workflowTaskId = null;
        String draftId = UUID.randomUUID().toString();
        if (workflowService.isWorkflowEnabled(serviceArtifactType) && workflowService
                .getDefaultWorkflow(serviceArtifactType, WorkflowType.PUBLISH, userDetails) != null) {

            pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.UPDATE,
                    userDetails);
            workflowTaskId = pi.getId();

        }
        domainRepository.createDraftFromPublished(publishedId, draftId, workflowTaskId, userDetails);

        addDomainLinks(draftId, current.getEntity(), userDetails);
        tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

        return draftId;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Domain updateDomain(String domainId, UpdatableDomainEntity domainEntity, UserDetails userDetails)
            throws LottabyteException {
        if (!domainRepository.domainExists(domainId,
                new ArtifactState[] { ArtifactState.PUBLISHED, ArtifactState.DRAFT }, userDetails))
            throw new LottabyteException(
                    Message.LBE00101,
                            userDetails.getLanguage(),
                    domainId);
        if (userDetails.getStewardId() != null && !domainRepository.hasAccessToDomain(domainId, userDetails))
            throw new LottabyteException(
                    Message.LBE00113,
                            userDetails.getLanguage(),
                    domainId);
        Domain current = domainRepository.getById(domainId, userDetails);
        String draftId = null;
        if (ArtifactState.PUBLISHED.equals(((WorkflowableMetadata) current.getMetadata()).getState())) {
            draftId = domainRepository.getDraftId(domainId, userDetails);
            if (draftId != null && !draftId.isEmpty())
                throw new LottabyteException(
                        Message.LBE00119,
                                userDetails.getLanguage(),
                        draftId);
        }

        validateSystemIds(domainId, domainEntity, current.getEntity(), userDetails);
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
            domainRepository.createDomainDraft(domainId, draftId, workflowTaskId, userDetails);
            addDomainLinks(draftId, current.getEntity(), userDetails);
            tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

        } else {
            draftId = domainId;
        }
        domainRepository.updateDomain(draftId, domainEntity, userDetails);
        if (domainEntity.getSystemIds() != null && !domainEntity.getSystemIds().isEmpty())
            updateDomainSystems(draftId, domainEntity.getSystemIds(), current.getEntity().getSystemIds(), userDetails);
        return domainRepository.getById(draftId, userDetails);
    }

    private String createDraftDomain(Domain current, WorkflowState workflowState, UserDetails userDetails)
            throws LottabyteException {
        ProcessInstance pi = null;
        String draftId = null;
        String workflowTaskId = null;

        draftId = UUID.randomUUID().toString();
        pi = workflowService.startFlowableProcess(draftId, serviceArtifactType, ArtifactAction.REMOVE, userDetails);
        workflowTaskId = pi.getId();

        draftId = domainRepository.createDomainDraft(current.getId(), draftId, workflowTaskId, userDetails);
        addDomainLinks(draftId, current.getEntity(), userDetails);
        tagService.mergeTags(current.getId(), serviceArtifactType, draftId, serviceArtifactType, userDetails);

        return draftId;
    }

    private void updateDomainSystems(String domainId, List<String> ids, List<String> currentIds,
            UserDetails userDetails) {
        ids.stream().filter(x -> !currentIds.contains(x)).collect(Collectors.toList())
                .forEach(y -> domainRepository.addSystemToDomain(y, domainId, userDetails));
        currentIds.stream().filter(x -> !ids.contains(x)).collect(Collectors.toList())
                .forEach(y -> domainRepository.removeSystemFromDomain(y, domainId, userDetails));
    }

    private void updateDomainStewards(String domainId, List<String> ids, List<String> currentIds,
            UserDetails userDetails) throws LottabyteException {
        for (String s : ids.stream().filter(x -> !currentIds.contains(x)).collect(Collectors.toList())) {
            stewardService.addStewardToDomain(s, domainId, false, userDetails);
        }
        for (String s : currentIds.stream().filter(x -> !ids.contains(x)).collect(Collectors.toList())) {
            stewardService.removeStewardFromDomain(s, domainId, false, userDetails);
        }
    }

    public void validateAccessToDomains(List<String> domainIds, UserDetails userDetails) throws LottabyteException {
        if (userDetails.getStewardId() != null && domainIds != null) {
            List<String> erroredDomains = new ArrayList<>();
            for (String s : domainIds) {
                if (!domainRepository.hasAccessToDomain(s, userDetails))
                    erroredDomains.add(s);
            }
            if (!erroredDomains.isEmpty())
                throw new LottabyteException(
                        Message.LBE00920,
                                userDetails.getLanguage(),
                        String.join(", ", erroredDomains));
        }
    }

    private void validateStewardsIds(String domainId, UpdatableDomainEntity domainEntity, UserDetails userDetails)
            throws LottabyteException {
        if (domainEntity.getStewards() != null && !domainEntity.getStewards().isEmpty()) {
            List<String> unknownStewards = new ArrayList<>();
            for (String stewardId : domainEntity.getStewards()) {
                if (!stewardService.stewardExists(stewardId, userDetails))
                    unknownStewards.add(stewardId);
            }
            if (!unknownStewards.isEmpty())
                throw new LottabyteException(
                        Message.LBE00204,
                                userDetails.getLanguage(),
                        String.join(", ", unknownStewards));
        }
    }

    private void validateSystemIds(String domainId, UpdatableDomainEntity domainEntity, DomainEntity currentEntity,
            UserDetails userDetails) throws LottabyteException {
        List<String> currentSystems = new ArrayList<>();
        if (currentEntity != null && currentEntity.getSystemIds() != null)
            currentSystems = currentEntity.getSystemIds();
        if (domainEntity.getSystemIds() != null) {
            List<String> erroredSystems = new ArrayList<>();
            for (String s : domainEntity.getSystemIds()) {
                if (!systemRepository.existsByIdAndPublished(s, userDetails))
                    erroredSystems.add(s);
            }
            if (!erroredSystems.isEmpty())
                throw new LottabyteException(
                        Message.LBE00114,
                                userDetails.getLanguage(),
                        String.join(", ", erroredSystems));
            List<String> systemIdsToDelete = currentSystems.stream()
                    .filter(x -> !domainEntity.getSystemIds().contains(x))
                    .collect(Collectors.toList());
            if (systemIdsToDelete.stream()
                    .anyMatch(x -> dataAssetRepository.existsDataAssetWithSystemAndDomain(x, domainId, userDetails))) {
                List<String> errors = new ArrayList<>();
                List<String> erroredDomainsIds = new ArrayList<>();
                for (String systemId : systemIdsToDelete) {
                    List<DataAsset> dataAssets = dataAssetRepository.getDataAssetsBySystemAndDomain(systemId, domainId,
                            userDetails);
                    if (!dataAssets.isEmpty()) {
                        dataAssets.stream().forEach(x -> errors.add(x.getId() + " (" + x.getName() + ")"));
                        erroredDomainsIds.add(domainId);
                    }
                }
                throw new LottabyteException(
                        Message.LBE00110,
                                userDetails.getLanguage(),
                        String.join(", ", erroredDomainsIds), String.join(", ", errors));
            }
        }
    }

    public PaginatedArtifactList<Domain> getDomainsPaginated(Integer offset, Integer limit, String artifactState,
            UserDetails userDetails) throws LottabyteException {
        if (!EnumUtils.isValidEnum(ArtifactState.class, artifactState))
            throw new LottabyteException(
                    Message.LBE00067,
                            userDetails.getLanguage(),
                    artifactState);
        PaginatedArtifactList<Domain> domainPaginatedArtifactList = domainRepository.getAllPaginated(offset, limit,
                "/v1/domains/", ArtifactState.valueOf(artifactState), userDetails);
        domainPaginatedArtifactList.getResources().forEach(
                domain -> domain.getMetadata().setTags(tagService.getArtifactTags(domain.getId(), userDetails)));
        return domainPaginatedArtifactList;
    }

    public PaginatedArtifactList<Domain> getDomainVersions(String domainId, Integer offset, Integer limit,
            UserDetails userDetails) throws LottabyteException {
        if (!domainRepository.domainExists(domainId,
                new ArtifactState[] { ArtifactState.PUBLISHED, ArtifactState.REMOVED }, userDetails)) {
            PaginatedArtifactList<Domain> res = new PaginatedArtifactList<>();
            res.setCount(0);
            res.setOffset(offset);
            res.setLimit(limit);
            res.setResources(new ArrayList<>());
            return res;
            // throw new LottabyteException(HttpStatus.BAD_REQUEST, Message.LBE00101,
            // domainId);
        }
        PaginatedArtifactList<Domain> domainPaginatedArtifactList = domainRepository.getVersionsById(domainId, offset,
                limit, "/v1/domains/" + domainId + "/versions", userDetails);
        // List<Tag> tagList = tagService.getArtifactTags(domainId, userDetails);
        // domainPaginatedArtifactList.getResources().forEach(domain ->
        // domain.getMetadata().setTags(tagList));
        for (Domain d : domainPaginatedArtifactList.getResources()) {
            // d.getMetadata().setTags(ta);
            fillDomainRelations(d, userDetails);
        }
        return domainPaginatedArtifactList;
    }

    private void fillDomainRelations(Domain d, UserDetails userDetails) {
        WorkflowableMetadata md = (WorkflowableMetadata) d.getMetadata();
        if (md.getAncestorDraftId() != null) {
            d.getMetadata().setTags(tagService.getArtifactTags(md.getAncestorDraftId(), userDetails));
            d.getEntity().setSystemIds(domainRepository.getSystemIdsByDomainId(md.getAncestorDraftId(), userDetails));
        }
    }

    public SearchResponse<FlatDomain> searchDomains(SearchRequestWithJoin request, UserDetails userDetails)
            throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        ServiceUtils.validateSearchRequestWithJoinState(request, userDetails);
        SearchResponse<FlatDomain> res = domainRepository.searchDomains(request, searchableColumns, userDetails);
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
        res.getItems().forEach(d -> d.setStewards(
                stewardService.getStewardsByDomainId(d.getId(), userDetails)
                        .stream().map(x -> FlatRelation.builder()
                                .id(x.getId())
                                .name(x.getName())
                                .url("/v1/stewards/" + x.getId())
                                .build())
                        .collect(Collectors.toList())));
        return res;
    }

    public List<Domain> getDomainsBySystemId(String systemId, UserDetails userDetails) {
        return domainRepository.getDomainsBySystemId(systemId, userDetails);
    }

    public SearchableDomain getSearchableArtifact(Domain d, UserDetails userDetails) {
        SearchableDomain sa = SearchableDomain.builder()
        .id(d.getMetadata().getId())
        .versionId(d.getMetadata().getVersionId())
        .name(d.getMetadata().getName())
        .description(d.getEntity().getDescription())
        .modifiedBy(d.getMetadata().getModifiedBy())
        .modifiedAt(d.getMetadata().getModifiedAt())
        .artifactType(d.getMetadata().getArtifactType())
        .effectiveStartDate(d.getMetadata().getEffectiveStartDate())
        .effectiveEndDate(d.getMetadata().getEffectiveEndDate())
        .tags(Helper.getEmptyListIfNull(d.getMetadata().getTags()).stream()
                .map(x -> x.getName()).collect(Collectors.toList()))
        .domains(Collections.singletonList(d.getId().toString()))

        .stewards(d.getEntity().getStewards()).build();

        return sa;
    }
}
