package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.ArchiveResponse;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.PaginatedArtifactList;
import ru.bssg.lottabyte.core.model.entityQuery.EntityQuery;
import ru.bssg.lottabyte.core.model.search.SearchableArtifact;
import ru.bssg.lottabyte.core.model.system.SearchableSystem;
import ru.bssg.lottabyte.core.model.system.System;
import ru.bssg.lottabyte.core.model.system.SystemConnection;
import ru.bssg.lottabyte.core.model.task.FlatTask;
import ru.bssg.lottabyte.core.model.task.Task;
import ru.bssg.lottabyte.core.model.task.UpdatableTaskEntity;
import ru.bssg.lottabyte.core.ui.model.SearchColumn;
import ru.bssg.lottabyte.core.ui.model.SearchColumnForJoin;
import ru.bssg.lottabyte.core.ui.model.SearchRequestWithJoin;
import ru.bssg.lottabyte.core.ui.model.SearchResponse;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.util.ServiceUtils;
import ru.bssg.lottabyte.coreapi.repository.TaskRepository;
import ru.bssg.lottabyte.coreapi.util.AllValidator;
import ru.bssg.lottabyte.coreapi.util.Helper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final ElasticsearchService elasticsearchService;
    private final EntityQueryService entityQueryService;
    private final SystemConnectionService systemConnectionService;

    private final SearchColumn[] searchableColumns = {
            new SearchColumn("name", SearchColumn.ColumnType.Text),
            new SearchColumn("description", SearchColumn.ColumnType.Text),
            /*new SearchColumn("task_run.last_updated", SearchColumn.ColumnType.Timestamp),
            new SearchColumn("task_run.task_state", SearchColumn.ColumnType.Text),*/
            new SearchColumn("system_connection_name", SearchColumn.ColumnType.Text),
            new SearchColumn("query_name", SearchColumn.ColumnType.Text),
            new SearchColumn("last_updated", SearchColumn.ColumnType.Timestamp),
            new SearchColumn("task_state", SearchColumn.ColumnType.Text)
    };

    private final SearchColumnForJoin[] joinColumns = {
            new SearchColumnForJoin("domain_id", "system_to_domain", SearchColumn.ColumnType.UUID, "id", "system_id")
    };

    public boolean hasAccessToTask(String taskId, UserDetails userDetails) {
        return taskRepository.hasAccessToTask(taskId, userDetails);
    }

    public Task getTaskById(String taskId, UserDetails userDetails) throws LottabyteException {
        Task task = taskRepository.getById(taskId, userDetails);
        if (task == null)
            throw new LottabyteException(Message.LBE01401, userDetails.getLanguage(), taskId);
        return task;
    }

    public PaginatedArtifactList<Task> getTasksWithPaging(Integer limit, Integer offset, UserDetails userDetails){
        return taskRepository.getAllPaginated(limit, offset, "/v1/task", userDetails);
    }

    public PaginatedArtifactList<Task> getTasksByQueryId(String queryId, Integer limit, Integer offset, UserDetails userDetails){
        return taskRepository.getTasksByQueryId(queryId, limit, offset, userDetails);
    }

    public Task createTask(UpdatableTaskEntity newTaskEntity, UserDetails userDetails) throws LottabyteException {
        if ((newTaskEntity.getScheduleType() != null && newTaskEntity.getScheduleParams() == null) || newTaskEntity.getScheduleType() == null && newTaskEntity.getScheduleParams() != null)
            throw new LottabyteException(Message.LBE01402, userDetails.getLanguage());
        if (!AllValidator.dateValidator(newTaskEntity))
            throw new LottabyteException(Message.LBE01403, userDetails.getLanguage(), newTaskEntity.getScheduleParams(), newTaskEntity.getScheduleType());
        if (newTaskEntity.getQueryId() == null)
            throw new LottabyteException(Message.LBE00402, userDetails.getLanguage());
        if (newTaskEntity.getSystemConnectionId() == null)
            throw new LottabyteException(Message.LBE01203, userDetails.getLanguage());
        if(newTaskEntity.getName() == null || newTaskEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE01406, userDetails.getLanguage());

        EntityQuery entityQuery = entityQueryService.getEntityQueryById(newTaskEntity.getQueryId(), userDetails);
        SystemConnection systemConnection = systemConnectionService.getSystemConnectionById(newTaskEntity.getSystemConnectionId(), userDetails);
        if (entityQuery == null)
            throw new LottabyteException(Message.LBE00401, userDetails.getLanguage(), newTaskEntity.getQueryId());
        if (userDetails.getStewardId() != null && !entityQueryService.hasAccessToQuery(entityQuery.getId(), userDetails))
            throw new LottabyteException(Message.LBE00403, userDetails.getLanguage(), entityQuery.getId());

        if (systemConnection == null)
            throw new LottabyteException(Message.LBE01201, userDetails.getLanguage(), newTaskEntity.getSystemConnectionId());
        if (userDetails.getStewardId() != null && !systemConnectionService.hasAccessToSystemConnection(systemConnection.getId(), userDetails))
            throw new LottabyteException(Message.LBE01208, userDetails.getLanguage(), systemConnection.getId());

        if (!entityQuery.getEntity().getSystemId().equals(systemConnection.getEntity().getSystemId()))
            throw new LottabyteException(Message.LBE01404, userDetails.getLanguage(), entityQuery.getEntity().getSystemId(), systemConnection.getEntity().getSystemId());

        String uuid = taskRepository.createTask(newTaskEntity, userDetails);
        Task task = getTaskById(uuid, userDetails);
        elasticsearchService.insertElasticSearchEntity(Collections.singletonList(getSearchableArtifact(task, userDetails)), userDetails);
        return task;
    }

    public Task getTaskByName(String systemFolderName, UserDetails userDetails) {
        return taskRepository.getTaskByName(systemFolderName, userDetails);
    }

    public Task updateTask(String taskId, UpdatableTaskEntity taskEntity, UserDetails userDetails) throws LottabyteException {
        SystemConnection systemConnection = null;
        EntityQuery entityQuery = null;
        if (getTaskById(taskId, userDetails) == null)
            throw new LottabyteException(Message.LBE01401, userDetails.getLanguage(), taskId);
        if (userDetails.getStewardId() != null && !taskRepository.hasAccessToTask(taskId, userDetails))
            throw new LottabyteException(Message.LBE01408, userDetails.getLanguage(), taskId);
        if ((taskEntity.getScheduleType() != null && taskEntity.getScheduleParams() == null) || taskEntity.getScheduleType() == null && taskEntity.getScheduleParams() != null)
            throw new LottabyteException(Message.LBE01402, userDetails.getLanguage());
        if (taskEntity.getScheduleParams() != null && !AllValidator.dateValidator(taskEntity))
            throw new LottabyteException(Message.LBE01403, userDetails.getLanguage(), taskEntity.getScheduleParams(), taskEntity.getScheduleType());
        if (taskEntity.getQueryId() != null){
            entityQuery = entityQueryService.getEntityQueryById(taskEntity.getQueryId(), userDetails);
            if(entityQuery == null)
                throw new LottabyteException(Message.LBE00401, userDetails.getLanguage(), taskEntity.getQueryId());
            if (userDetails.getStewardId() != null && !entityQueryService.hasAccessToQuery(entityQuery.getId(), userDetails))
                throw new LottabyteException(Message.LBE00403, userDetails.getLanguage(), entityQuery.getId());
        }
        if (taskEntity.getSystemConnectionId() != null){
            systemConnection = systemConnectionService.getSystemConnectionById(taskEntity.getSystemConnectionId(), userDetails);
            if (systemConnection == null)
                throw new LottabyteException(Message.LBE01201, userDetails.getLanguage(), taskEntity.getSystemConnectionId());
            if (userDetails.getStewardId() != null && !systemConnectionService.hasAccessToSystemConnection(systemConnection.getId(), userDetails))
                throw new LottabyteException(Message.LBE01208, userDetails.getLanguage(), systemConnection.getId());
        }
        if(taskEntity.getQueryId() != null && taskEntity.getSystemConnectionId() != null){
            if (!entityQuery.getEntity().getSystemId().equals(systemConnection.getEntity().getSystemId()))
                throw new LottabyteException(Message.LBE01404, userDetails.getLanguage(), entityQuery.getEntity().getSystemId(), systemConnection.getEntity().getSystemId());
        }
        if(taskEntity.getName() != null && taskEntity.getName().isEmpty())
            throw new LottabyteException(Message.LBE01406, userDetails.getLanguage());


        taskRepository.updateTask(taskId, taskEntity, userDetails);
        Task task = getTaskById(taskId, userDetails);
        elasticsearchService.updateElasticSearchEntity(Collections.singletonList(getSearchableArtifact(task, userDetails)), userDetails);
        return task;
    }

    public ArchiveResponse deleteTask(String taskId, UserDetails userDetails) throws LottabyteException {
        if (getTaskById(taskId, userDetails) == null)
            throw new LottabyteException(Message.LBE01401, userDetails.getLanguage(), taskId);

        if (userDetails.getStewardId() != null && !taskRepository.hasAccessToTask(taskId, userDetails))
            throw new LottabyteException(Message.LBE01408, userDetails.getLanguage(), taskId);
        taskRepository.deleteById(taskId, userDetails);

        ArchiveResponse archiveResponse = new ArchiveResponse();
        archiveResponse.setDeletedGuids(Collections.singletonList(taskId));
        elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(taskId), userDetails);
        return archiveResponse;
    }
    public ArchiveResponse deleteTasksByQueryId(String queryId, UserDetails userDetails) throws LottabyteException {
        List<Task> taskList = getTasksByQueryId(queryId, 0, 10000, userDetails).getResources();
        if (taskList == null || taskList.isEmpty())
            throw new LottabyteException(Message.LBE01409, userDetails.getLanguage(), queryId);

        if (userDetails.getStewardId() != null && !taskRepository.hasAccessToTask(queryId, userDetails))
            throw new LottabyteException(Message.LBE01408, userDetails.getLanguage(), queryId);
        taskRepository.deleteTasksByQueryId(queryId, userDetails);

        ArchiveResponse archiveResponse = new ArchiveResponse();
        archiveResponse.setDeletedGuids(Collections.singletonList(queryId));
        elasticsearchService.deleteElasticSearchEntityById(Collections.singletonList(queryId), userDetails);
        return archiveResponse;
    }

    public SearchResponse<FlatTask> searchTaskRuns(SearchRequestWithJoin request, UserDetails userDetails) throws LottabyteException {
        ServiceUtils.validateSearchRequestWithJoin(request, searchableColumns, joinColumns, userDetails);
        return taskRepository.searchTaskRuns(request, searchableColumns, joinColumns, userDetails);
    }

    public SearchableArtifact getSearchableArtifact(Task task, UserDetails userDetails) {
        SearchableArtifact sa = new SearchableArtifact();
        sa.setId(task.getMetadata().getId());
        sa.setVersionId(task.getMetadata().getVersionId());
        sa.setName(task.getMetadata().getName());
        sa.setDescription(task.getEntity().getDescription());
        sa.setModifiedBy(task.getMetadata().getModifiedBy());
        sa.setModifiedAt(task.getMetadata().getModifiedAt());
        sa.setArtifactType(task.getMetadata().getArtifactType());
        sa.setEffectiveStartDate(task.getMetadata().getEffectiveStartDate());
        sa.setEffectiveEndDate(task.getMetadata().getEffectiveEndDate());
        sa.setTags(Helper.getEmptyListIfNull(task.getMetadata().getTags()).stream()
                .map(x -> x.getName()).collect(Collectors.toList()));

        sa.setDomains(taskRepository.getDomainIdsByTaskId(task.getId(), userDetails));

        return sa;
    }
}
