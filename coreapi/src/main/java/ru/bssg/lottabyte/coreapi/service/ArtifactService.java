package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.workflow.WorkflowTask;
import ru.bssg.lottabyte.core.ui.model.*;
import ru.bssg.lottabyte.core.ui.model.dashboard.DashboardEntity;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelData;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelNodeData;
import ru.bssg.lottabyte.core.ui.model.gojs.UpdatableGojsModelData;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.ArtifactRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArtifactService {
    private final ArtifactRepository artifactRepository;
    private final ReferenceService referenceService;
    private final RuntimeService runtimeService;
    private final WorkflowService workflowService;
    private final UserService userService;
    private final TagService tagService;
    private final UserFavService userFavService;

    private final SearchColumn[] searchableColumns = {
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            new SearchColumn("short_description", SearchColumn.ColumnType.Text),
            new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
            new SearchColumn("stewards", SearchColumn.ColumnType.Array),
            new SearchColumn("tags", SearchColumn.ColumnType.Text),
            //new SearchColumn("workflow_state", SearchColumn.ColumnType.Text)
    };

    private final SearchColumnForJoin[] joinColumns = {};

    private final ArtifactsRelation[] artifactRelations = new ArtifactsRelation[] {
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.domain, ArtifactType.indicator, null, "domain_id", null),
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.domain, ArtifactType.business_entity, null, "domain_id", null),
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.domain, ArtifactType.product, null, "domain_id", null),
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.CrossTable, ArtifactType.domain, ArtifactType.system, "domain_id", "system_id", "system_to_domain"),
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.domain, ArtifactType.data_asset, null, "domain_id", null),

        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.CrossTable, ArtifactType.entity, ArtifactType.system, "entity_id", "system_id", "entity_to_system"),
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.system, ArtifactType.entity_query, null, "system_id", null),
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.system, ArtifactType.entity_sample, null, "system_id", null),
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.system, ArtifactType.data_asset, null, "system_id", null),

        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.entity, ArtifactType.entity_attribute, null, "entity_id", null),
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.entity, ArtifactType.entity_sample, null, "entity_id", null),
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.entity, ArtifactType.data_asset, null, "entity_id", null),

        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.entity_query, ArtifactType.task, null, "query_id", null),
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.entity_query, ArtifactType.entity_sample, null, "entity_query_id", null),
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.entity_query, ArtifactType.product, null, "entity_query_id", null),

        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.system, ArtifactType.meta_database, null, "system_id", null),

        //new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.product, ArtifactType.product, null, "p")
        //new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.ForeignKey, ArtifactType.product, ArtifactType.product, null, "p")


        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.CustomSQL, ArtifactType.domain, ArtifactType.entity,
                "JOIN da_{tenant}.entity_to_system t1 ON t1.entity_id=tbl1.id JOIN da_{tenant}.system_to_domain t2 ON t1.system_id=t2.system_id",
                "t2.domain_id={srcArtifactId}"),
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.CustomSQL, ArtifactType.domain, ArtifactType.entity_query,
                "JOIN da_{tenant}.system_to_domain t2 ON t2.system_id=tbl1.system_id",
                "t2.domain_id={srcArtifactId}"),
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.CustomSQL, ArtifactType.domain, ArtifactType.entity_sample,
                "JOIN da_{tenant}.system_to_domain t2 ON t2.system_id=tbl1.system_id",
                "t2.domain_id={srcArtifactId}"),

        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.CustomSQL, ArtifactType.data_asset, ArtifactType.entity_attribute,
                "JOIN da_{tenant}.data_asset da ON tbl1.entity_id=da.entity_id", "da.id={srcArtifactId}"),
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.CustomSQL, ArtifactType.data_asset, ArtifactType.entity_sample,
                "JOIN da_{tenant}.data_asset da ON tbl1.entity_id=da.entity_id AND tbl1.system_id=da.system_id",
                "da.id={srcArtifactId}"),

        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.CustomSQL, ArtifactType.business_entity, ArtifactType.data_asset,
                "", "tbl1.entity_id IN (SELECT ee.id FROM da_{tenant}.entity ee JOIN da_{tenant}.reference rr ON ee.id=rr.source_id AND rr.target_id={srcArtifactId})"),

        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.CustomSQL, ArtifactType.indicator, ArtifactType.entity,
                "JOIN da_{tenant}.data_asset da ON da.entity_id=tbl1.id AND da.state='PUBLISHED' JOIN da_{tenant}.reference r1 ON r1.target_id=da.id AND r1.reference_type='INDICATOR_TO_DATA_ASSET'",
                "r1.source_id={srcArtifactId}"),
        new ArtifactsRelation(ArtifactsRelation.ArtifactsRelationType.CustomSQL, ArtifactType.product, ArtifactType.entity_sample_property,
                "JOIN da_{tenant}.entity_attribute_to_sample_property ea2sp ON ea2sp.entity_sample_property_id=tbl1.id "
                + "JOIN da_{tenant}.entity_attribute ea ON ea2sp.entity_attribute_id=ea.id "
                + "JOIN da_{tenant}.entity e ON ea.entity_id=e.id "
                + "JOIN da_{tenant}.entity_query q ON q.entity_id=e.id "
                + "JOIN da_{tenant}.entity_sample es ON tbl1.entity_sample_id=es.id AND e.id=es.entity_id AND es.system_id=q.system_id ",

                "q.id = (SELECT entity_query_id FROM da_{tenant}.product WHERE id={srcArtifactId})")
    };

    public Integer getSettingsCount(String type, UserDetails userDetails) {
        return artifactRepository.getSettingsCount(type, userDetails);
    }

    public Map<String, Integer> getArtifactsCount(List<String> artifactTypes, Boolean limitSteward, UserDetails userDetails) {
        Map<String, Integer> artifactTypeIntegerMap = new HashMap<>();
        for (String artifactType : artifactTypes) {
            artifactTypeIntegerMap.put(artifactType, artifactRepository.getArtifactsCount(artifactType, limitSteward, userDetails));
        }
        return artifactTypeIntegerMap;
    }

    public GojsModelData getModel(String artifactType, UserDetails userDetails) {
        return artifactRepository.getModel(artifactType, userDetails);
    }

    public GojsModelData getArtifactModel(String artifactId, String artifactType, UserDetails userDetails) {
        return artifactRepository.getArtifactModel(artifactId, artifactType, userDetails);
    }

    public List<GojsModelNodeData> updateModel(UpdatableGojsModelData updatableGojsModelData, UserDetails userDetails) {
        return artifactRepository.updateModel(updatableGojsModelData, userDetails);
    }

    public List<GojsModelNodeData> updateArtifactModel(UpdatableGojsModelData updatableGojsModelData, String artifactType, String artifactId, UserDetails userDetails) throws LottabyteException {
        return artifactRepository.updateArtifactModel(updatableGojsModelData, artifactType, artifactId, userDetails);
    }

    public List<DashboardEntity> getDashboard(UserDetails userDetails) {
        return artifactRepository.getDashboard(userDetails);
    }

    public List<DashboardEntity> getRecommended(UserDetails userDetails) {
        List<DashboardEntity> res = artifactRepository.getRecommended(userDetails);
        res.forEach(d -> d.setIsInFav(userFavService.isInFav(d.getId(), userDetails)));
        return res;
    }

    public List<DashboardEntity> getPopular(UserDetails userDetails) {
        return artifactRepository.getPopular(userDetails);
    }

    public List<DashboardEntity> getFavorites(UserDetails userDetails) {
        return artifactRepository.getFavorites(userDetails);
    }

    public void clearModels(UserDetails userDetails) {
        artifactRepository.clearModels(userDetails);
    }

    public SearchResponse<FlatWFItemObject> searchDrafts(SearchRequest request, UserDetails userDetails)
            throws LottabyteException {
        SearchColumn[] searchableColumns = {
                new SearchColumn("tbl1.name", SearchColumn.ColumnType.Text),
                new SearchColumn("short_description", SearchColumn.ColumnType.Text),
                new SearchColumn("tbl1.modified", SearchColumn.ColumnType.Timestamp),
                new SearchColumn("tbl1.artifact_type", SearchColumn.ColumnType.Text),
                new SearchColumn("at.name", SearchColumn.ColumnType.Text),
                new SearchColumn("ws.name", SearchColumn.ColumnType.Text),

        };

        ServiceUtils.validateSearchRequest(request, searchableColumns, userDetails);

        SearchResponse<FlatWFItemObject> res = artifactRepository.searchDrafts(request, searchableColumns, userDetails);

        res.getItems().stream()
                .forEach(y -> {
                    y.setUserName("");
                    WorkflowTask task = workflowService.getWorkflowTaskById(y.getWorkflowTaskId(),
                            userDetails, false);
                    if (task != null) {
                        y.setWorkflowState(task.getEntity().getWorkflowState());
                        y.setWorkflowStateName(workflowService.getWorkflowStateName(task.getEntity().getWorkflowState(), userDetails));
                        y.setUserName(task.getEntity().getResponsible());
                        if (y.getUserName() == null || y.getUserName().isEmpty()) {
                            try {
                                y.setUserName((task.getCreatedBy() == null || task.getCreatedBy().isEmpty()) ? "" : userService.getUserById(task.getCreatedBy(), userDetails.getTenant()).getDisplayName());
                            } catch (LottabyteException e) {
                                y.setUserName("error");
                                log.error(e.getMessage(), e);
                            }
                        }
                    }
                });

        return res;
    }

    public Map<String, String> getArtifactTypes(boolean onlyWorkflowEnabled, UserDetails userDetails) {
        Map<String, String> res = artifactRepository.getArtifactTypes(userDetails);
        if (onlyWorkflowEnabled) {
            Map<String, String> res2 = new HashMap<>();
            for (Map.Entry<String, String> entry : res.entrySet()) {
                ArtifactType at = ArtifactType.valueOf(entry.getKey());

                if (workflowService.isWorkflowEnabled(at)) {
                    res2.put(entry.getKey(), entry.getValue());
                }
            }

            return res2;
        } else
        return res;
    }

    public String getArtifactType(String code, UserDetails userDetails) {
        return artifactRepository.getArtifactType(code, userDetails);
    }

    public List<String> getArtifactActions(UserDetails userDetails) {
        return Arrays.stream(ArtifactAction.values()).map(x -> x.name()).collect(Collectors.toList());
    }

    public void updateModelsWithArtifact(String id, UserDetails userDetails) {
        artifactRepository.updateModelsWithArtifact(id, userDetails);
    }

    public void updateModelForArtifact(String id, String artifactType, UserDetails userDetails) {
        artifactRepository.updateModelForArtifact(id, artifactType, userDetails);
    }

    public List<String> getRelatedArtifactTypes(String artifactType, UserDetails userDetails) {
        List<String> res = referenceService.getReferencedArtifactTypes(artifactType, userDetails);

        for (ArtifactsRelation ar : artifactRelations) {
            if (ar.getArtifact1Type().toString().equals(artifactType) && !res.contains(ar.getArtifact2Type().toString()))
                res.add(ar.getArtifact2Type().toString());
            if (ar.getArtifact2Type().toString().equals(artifactType) && !ar.getRelationType().equals(ArtifactsRelation.ArtifactsRelationType.ForeignKey)
                && !ar.getRelationType().equals(ArtifactsRelation.ArtifactsRelationType.CustomSQL) && !res.contains(ar.getArtifact1Type().toString()))
                res.add(ar.getArtifact1Type().toString());
        }

        return res.stream().filter(x -> !x.equals(ArtifactType.entity_attribute.name())).collect(Collectors.toList());
    }

    public SearchResponse<? extends FlatModeledObject> searchRelatedArtifacts(String srcArtifactType, String srcArtifactId, String tgtArtifactType, SearchRequestWithJoin request, UserDetails userDetails)
            throws LottabyteException {
        //ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        //ServiceUtils.validateSearchRequestWithJoinState(request, userDetails);

        SearchResponse<? extends FlatModeledObject> res = null;

        SearchColumn[] cols;
        if (tgtArtifactType.equals("meta_database"))
            cols = new SearchColumn[] {
                    new SearchColumn("name", SearchColumn.ColumnType.Text),
                    new SearchColumn("description", SearchColumn.ColumnType.Text),
                    new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
                    new SearchColumn("tags", SearchColumn.ColumnType.Text),
                    new SearchColumn("driver_class_name", SearchColumn.ColumnType.Text),
                    new SearchColumn("jdbc_url", SearchColumn.ColumnType.Text)
            };
        else
            cols = new SearchColumn[] {
                    new SearchColumn("name", SearchColumn.ColumnType.Text),
                    new SearchColumn("description", SearchColumn.ColumnType.Text),
                    new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
                    new SearchColumn("tags", SearchColumn.ColumnType.Text)
            };

        for (ArtifactsRelation ar : artifactRelations) {
            if ((ar.getArtifact1Type().toString().equals(srcArtifactType) && ar.getArtifact2Type().toString().equals(tgtArtifactType))
                || (ar.getArtifact1Type().toString().equals(tgtArtifactType) && ar.getArtifact2Type().toString().equals(srcArtifactType) && !ar.getRelationType().equals(ArtifactsRelation.ArtifactsRelationType.CustomSQL)))
                res = artifactRepository.searchRelatedArtifacts(srcArtifactType, srcArtifactId, tgtArtifactType, ar, request, cols, userDetails);
        }

        if (res == null)
            res = artifactRepository.searchRelatedArtifacts(srcArtifactType, srcArtifactId, tgtArtifactType, null, request, cols, userDetails);
        res.getItems().stream().forEach(
                x -> { if (x instanceof IFlatModeledObjectWithTags) ((IFlatModeledObjectWithTags)x).setTags(tagService.getArtifactTags(x.getId(), userDetails)
                        .stream().map(y -> y.getName()).collect(Collectors.toList())); });
        /*res.getItems().stream()
                .filter(x -> ArtifactState.DRAFT.equals(x.getState()) && x.getWorkflowTaskId() != null)
                .forEach(y -> {
                    WorkflowTask task = workflowService.getWorkflowTaskById(y.getWorkflowTaskId(), userDetails, false);
                    if (task != null)
                        y.setWorkflowState(task.getEntity().getWorkflowState());
                });*/
        /*res.getItems().forEach(d -> d.setStewards(
                stewardService.getStewardsByDomainId(d.getId(), userDetails)
                        .stream().map(x -> FlatRelation.builder()
                        .id(x.getId())
                        .name(x.getName())
                        .url("/v1/stewards/" + x.getId())
                        .build())
                        .collect(Collectors.toList())));*/
        return res;
    }

    public UploadedFilesListData uploadImage(MultipartFile[] files, String folder) {
        UploadedFilesListData res = new UploadedFilesListData();

        try {

            if (files != null) {
                List<UploadedFileData> list = new ArrayList<>();

                for (MultipartFile file : files) {
                    list.add(artifactRepository.uploadImage(file, folder));
                }

                res.setResult(list);
            }
        } catch (LottabyteException e) {
            res.setErrorMessage(e.getMessage());
        }

        return res;
    }

    public ArtifactRepository.ServeFileData getImage(String filename, String folder) throws LottabyteException {
        return artifactRepository.getImage(filename, folder);
    }
}
