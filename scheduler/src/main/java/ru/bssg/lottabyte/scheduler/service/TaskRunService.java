package ru.bssg.lottabyte.scheduler.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.model.taskrun.TaskRun;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.scheduler.repository.TaskRunRepository;

import java.util.List;

@Service
@Slf4j
public class TaskRunService {
    private final TaskRunRepository taskRunRepository;
    @Autowired
    public TaskRunService(TaskRunRepository taskRunRepository) {
        this.taskRunRepository = taskRunRepository;
    }

    public TaskRun getTaskRunByTaskScheduleId(String taskScheduleId, UserDetails userDetails) {
        return taskRunRepository.getTaskRunByTaskScheduleId(taskScheduleId, userDetails);
    }
    public List<TaskRun> getTaskRunListByTaskScheduleId(String taskScheduleId, UserDetails userDetails) {
        return taskRunRepository.getTaskRunListByTaskScheduleId(taskScheduleId, userDetails);
    }
}
