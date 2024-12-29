package ru.bssg.lottabyte.scheduler.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.task.Task;
import ru.bssg.lottabyte.core.model.task.UpdatableTaskEntity;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.scheduler.repository.TaskRepository;
import ru.bssg.lottabyte.scheduler.repository.TaskScheduleRepository;

import java.util.List;

@Service
@Slf4j
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskScheduleRepository taskScheduleRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository, TaskScheduleRepository taskScheduleRepository) {
        this.taskRepository = taskRepository;
        this.taskScheduleRepository = taskScheduleRepository;
    }
    public List<Task> getAllTasks(UserDetails userDetails) throws LottabyteException {
        List<Task> taskList = taskRepository.getAllTasks(userDetails);
        if (taskList == null || taskList.isEmpty())
            throw new LottabyteException(Message.LBE01407, userDetails.getLanguage());
        for (Task t : taskList)
            t.getEntity().setSchedules(taskScheduleRepository.getSchedules(t.getId(), userDetails));
        return taskList;
    }

    public void updateTaskScheduleEnabled(String taskScheduleId, UserDetails userDetails) {
        taskScheduleRepository.updateTaskScheduleEnabled(taskScheduleId, userDetails);
    }
}
