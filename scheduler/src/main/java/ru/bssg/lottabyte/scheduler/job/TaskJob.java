package ru.bssg.lottabyte.scheduler.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import ru.bssg.lottabyte.scheduler.client.LottabyteClient;

@Slf4j
public class TaskJob implements Job {
    @Override
    public void execute(JobExecutionContext jobContext) throws JobExecutionException {
        JobDetail jobDetail = jobContext.getJobDetail();
        String taskId = (String) jobDetail.getJobDataMap().get("taskId");
        LottabyteClient lottabyteClient = (LottabyteClient) jobDetail.getJobDataMap().get("lottabyteClient");

        lottabyteClient.postRunTask(taskId);
    }
}
