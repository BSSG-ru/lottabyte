package ru.bssg.lottabyte.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.taskrun.TaskRun;
import ru.bssg.lottabyte.core.model.taskrun.UpdatableTaskRunEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.coreapi.repository.TaskRunRepository;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskRunService {
    private final TaskRunRepository taskRunRepository;
    private final TaskService taskService;

    public List<TaskRun> getTaskRunListByTaskId(String taskId, UserDetails userDetails) {
        return taskRunRepository.getTaskRunListByTaskId(taskId, userDetails);
    }

    public TaskRun getTaskRunById(String taskRunId, UserDetails userDetails) {
        return taskRunRepository.getById(taskRunId, userDetails);
    }

    public TaskRun updateTaskRunById(String taskRunId, UpdatableTaskRunEntity updatableTaskRunEntity, UserDetails userDetails) {
        taskRunRepository.updateTaskRunById(taskRunId, updatableTaskRunEntity, userDetails);
        return getTaskRunById(taskRunId, userDetails);
    }

    public TaskRun createTaskRun(UpdatableTaskRunEntity updatableTaskRunEntity, UserDetails userDetails) throws LottabyteException {
        if (updatableTaskRunEntity.getTaskId() == null || taskService.getTaskById(updatableTaskRunEntity.getTaskId(), userDetails) == null)
            throw new LottabyteException(Message.LBE01401, userDetails.getLanguage(), updatableTaskRunEntity.getTaskId());

        String taskRunId = taskRunRepository.createTaskRun(updatableTaskRunEntity, userDetails);
        return getTaskRunById(taskRunId, userDetails);
    }
}
