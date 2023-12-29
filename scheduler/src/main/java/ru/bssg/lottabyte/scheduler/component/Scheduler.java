package ru.bssg.lottabyte.scheduler.component;

import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.scheduler.service.SchedulerService;

@Component
@Slf4j
public class Scheduler {
    private final SchedulerService schedulerService;

    @Autowired
    public Scheduler(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Scheduled(fixedRate = 60000)
    public void completingTasks() throws LottabyteException, SchedulerException {
        schedulerService.completingTasks();
    }
}
