package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.model.*;
import ru.bssg.lottabyte.core.model.domain.FlatDomain;
import ru.bssg.lottabyte.core.model.workflow.WorkflowTask;
import ru.bssg.lottabyte.core.ui.model.SearchColumn;
import ru.bssg.lottabyte.core.ui.model.SearchRequest;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.ui.model.dashboard.DashboardEntity;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelData;
import ru.bssg.lottabyte.core.ui.model.gojs.GojsModelNodeData;
import ru.bssg.lottabyte.core.ui.model.gojs.UpdatableGojsModelData;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.ArtifactRepository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArtifactService {
    private final ArtifactRepository artifactRepository;
    private final RuntimeService runtimeService;
    private final WorkflowService workflowService;
    private final UserService userService;

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

    public List<GojsModelNodeData> updateArtifactModel(UpdatableGojsModelData updatableGojsModelData, String artifactType, String artifactId, UserDetails userDetails) {
        return artifactRepository.updateArtifactModel(updatableGojsModelData, artifactType, artifactId, userDetails);
    }

    public List<DashboardEntity> getDashboard(UserDetails userDetails) {
        return artifactRepository.getDashboard(userDetails);
    }

    public void clearModels(UserDetails userDetails) {
        artifactRepository.clearModels(userDetails);
    }

    public SearchResponse<FlatWFItemObject> searchDrafts(SearchRequest request, UserDetails userDetails)
            throws LottabyteException {
        SearchColumn[] searchableColumns = {
                new SearchColumn("tbl1.name", SearchColumn.ColumnType.Text),
                new SearchColumn("description", SearchColumn.ColumnType.Text),
                new SearchColumn("modified", SearchColumn.ColumnType.Timestamp),
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
}
