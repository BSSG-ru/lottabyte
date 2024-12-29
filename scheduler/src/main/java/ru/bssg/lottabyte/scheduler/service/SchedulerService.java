package ru.bssg.lottabyte.scheduler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bssg.lottabyte.core.api.LottabyteException;
import ru.bssg.lottabyte.core.i18n.Message;
import ru.bssg.lottabyte.core.model.HttpStatus;
import ru.bssg.lottabyte.core.model.task.Task;
import ru.bssg.lottabyte.core.model.task.TaskSchedule;
import ru.bssg.lottabyte.core.model.taskrun.TaskRun;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.scheduler.client.LottabyteClient;
import ru.bssg.lottabyte.scheduler.job.TaskJob;
import ru.bssg.lottabyte.scheduler.util.Weekday;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;

import static org.quartz.CronExpression.isValidExpression;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static ru.bssg.lottabyte.core.model.task.TaskSchedulerType.*;

@Service
@Slf4j
public class SchedulerService {
    private final LottabyteClient lottabyteClient;
    private final TaskService taskService;
    private String bearerToken;
    private UserDetails userDetails;
    private final JwtHelper jwtHelper;
    private final TaskRunService taskRunService;
    private final Map<String, TaskSchedule> activeTaskSchedules;

    @Autowired
    public SchedulerService(LottabyteClient lottabyteClient, TaskService taskService, JwtHelper jwtHelper, TaskRunService taskRunService) {
        this.lottabyteClient = lottabyteClient;
        this.taskService = taskService;
        this.jwtHelper = jwtHelper;
        this.taskRunService = taskRunService;
        try {
            //this.bearerToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIjE0ZWJkOGVjLTA0MjItNDFjYS05ZTI4LTA5NmFkZDYxM2E0MCIsIjhmODIxYjNkLWZjNTUtNGE4Ny05NzdmLWNkMzg4ODVhOTFhMyIsImZlOGFiNzk5LWRmN2UtNDFhNC04OGI4LWIzMGZiNjZlYzAwMyJdLCJkb21haW5zIjpbXSwiZ3JvdXBzIjpbXSwibGFuZ3VhZ2UiOiJydSIsInVpZCI6IjEzMzIiLCJuYmYiOjE3Mjc3NzY3NTksImludGVybmFsVXNlciI6dHJ1ZSwicGVybWlzc2lvbnMiOlsidGFnX3UiLCJsb19yIiwibG9fdSIsImFjdGl2ZXNfd3JpdGUiLCJhY3RpdmVzX3JlYWQiLCJ0YWdfciIsInN0X3UiLCJzdF9yIiwiY3VzdG9tX2F0dHJpYnV0ZV91IiwidGVjaF9zcGVjX3IiLCJjdXN0b21fYXR0cmlidXRlX3IiLCJ0ZWNoX3NwZWNfdSIsInN5c3RlbV9yIiwic3lzdGVtX3UiLCJhcnRpZmFjdHNfciIsImxvX21kbF91IiwiYWN0aXZlX3IiLCJhcnRpZmFjdHNfdSIsImxvX21kbF9yIiwiYWN0aXZlX3UiLCJlbnVtZXJhdGlvbl9yIiwiZW51bWVyYXRpb25fdSIsInJhdGluZ191IiwicmF0aW5nX3IiLCJ0YXNrX3UiLCJidXNpbmVzc19lbnRpdHlfciIsInRhc2tfciIsImJ1c2luZXNzX2VudGl0eV91IiwiYWRtaW4iLCJjb25uZWN0b3JfciIsImVsYXN0aWNfc2VhcmNoX3IiLCJtZXRhZGF0YV9yIiwiZWxhc3RpY19zZWFyY2hfdSIsImluZGljYXRvcl9yIiwiZHFfcnVsZV9yIiwic2FtcGxlX3IiLCJpbmRpY2F0b3JfdSIsIm1ldGFkYXRhX3UiLCJkcV9ydWxlX3UiLCJzYW1wbGVfdSIsInJlcV91Iiwic2FtcGxlX2JvZHlfZG93bmxvYWQiLCJkb21haW5fciIsInJlcV9yIiwicXVhbGl0eV90YXNrX3IiLCJxdWFsaXR5X3Rhc2tfdSIsImRvbWFpbl91IiwicHJvZHVjdF91IiwicmVjZW50X3ZpZXdzX3IiLCJwcm9kdWN0X3IiLCJjb25uZWN0aW9uX3IiLCJ3b3JrZmxvd191IiwiY29tbWVudHNfdSIsImNvbm5lY3Rpb25fdSIsIndvcmtmbG93X3IiLCJjb21tZW50c19yIl0sImV4cCI6MTcyNzg2MzE1OSwiYXV0aGVudGljYXRvciI6ImRlZmF1bHQiLCJ0ZW5hbnQiOiIxMDIwIiwidXNlcm5hbWUiOiJhZG1pbiJ9.f7SPHaE8dgk_DakPwakuv0LJakdk4yPMk1mPnKw4BteOqZ0r5d8oDxImecg8APqtS2NMWwutTkphlRoEkyCNBjUtK-6aLJIThJLpY2rIASwR9MEYbBqgTgkjMoZ7t4-9HtVRBiN5cNGqtftkwE7WDXhX1ozMCIr4jj2pYw2KtLMOTC7equpLjrK5s8OzaKWr4IdpPr07B4UMZhmzDVXOe1w64LfNVLz-3Rx2-y4xS6cvKeNZoFPsWcq77lXFMThtVdJtPGx--S51VdoRsEKgxfbx-fWy7T-Nt74JqjLT6fYY0M1ozdZS06VJEmKk2pcXiSXfEiAEpLOoXJhpSz22HQ";//this.lottabyteClient.preauth();
            //this.bearerToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIjE0ZWJkOGVjLTA0MjItNDFjYS05ZTI4LTA5NmFkZDYxM2E0MCIsIjhmODIxYjNkLWZjNTUtNGE4Ny05NzdmLWNkMzg4ODVhOTFhMyIsImZlOGFiNzk5LWRmN2UtNDFhNC04OGI4LWIzMGZiNjZlYzAwMyJdLCJkb21haW5zIjpbXSwiZ3JvdXBzIjpbXSwibGFuZ3VhZ2UiOiJydSIsInVpZCI6IjEzMzIiLCJuYmYiOjE3MzM4MTU1MTYsImludGVybmFsVXNlciI6dHJ1ZSwicGVybWlzc2lvbnMiOlsidGFnX3UiLCJsb19yIiwibG9fdSIsImFjdGl2ZXNfd3JpdGUiLCJhY3RpdmVzX3JlYWQiLCJ0YWdfciIsInN0X3UiLCJzdF9yIiwiY3VzdG9tX2F0dHJpYnV0ZV91IiwidGVjaF9zcGVjX3IiLCJjdXN0b21fYXR0cmlidXRlX3IiLCJ0ZWNoX3NwZWNfdSIsInN5c3RlbV9yIiwic3lzdGVtX3UiLCJhcnRpZmFjdHNfciIsImxvX21kbF91IiwiYWN0aXZlX3IiLCJhcnRpZmFjdHNfdSIsImxvX21kbF9yIiwiYWN0aXZlX3UiLCJlbnVtZXJhdGlvbl9yIiwiZW51bWVyYXRpb25fdSIsInJhdGluZ191IiwicmF0aW5nX3IiLCJ0YXNrX3UiLCJidXNpbmVzc19lbnRpdHlfciIsInRhc2tfciIsImJ1c2luZXNzX2VudGl0eV91IiwiYWRtaW4iLCJjb25uZWN0b3JfciIsImVsYXN0aWNfc2VhcmNoX3IiLCJtZXRhZGF0YV9yIiwiZWxhc3RpY19zZWFyY2hfdSIsImluZGljYXRvcl9yIiwiZHFfcnVsZV9yIiwic2FtcGxlX3IiLCJpbmRpY2F0b3JfdSIsIm1ldGFkYXRhX3UiLCJkcV9ydWxlX3UiLCJzYW1wbGVfdSIsInJlcV91Iiwic2FtcGxlX2JvZHlfZG93bmxvYWQiLCJkb21haW5fciIsInJlcV9yIiwicXVhbGl0eV90YXNrX3IiLCJxdWFsaXR5X3Rhc2tfdSIsImRvbWFpbl91IiwicHJvZHVjdF91IiwicmVjZW50X3ZpZXdzX3IiLCJwcm9kdWN0X3IiLCJjb25uZWN0aW9uX3IiLCJ3b3JrZmxvd191IiwiY29tbWVudHNfdSIsImNvbm5lY3Rpb25fdSIsIndvcmtmbG93X3IiLCJjb21tZW50c19yIl0sImV4cCI6MTczMzkwMTkxNiwiYXV0aGVudGljYXRvciI6ImRlZmF1bHQiLCJ0ZW5hbnQiOiIxMDIwIiwidXNlcm5hbWUiOiJhZG1pbiJ9.GCDr7eJArTSviVkFhPrHP3n6jDd7INp0aUnTe5c8plBINtHOp5Tcr7IC8SdwNkrbht7mYqJSsTpCgsyM39CvdxRQGOoKKkoVxhAWXZFS2uc2zwu3UHLzJ3JzN3pl7rTV9lNfVILvedRKz9lNLalxwJIDWUPi5zo1FX3oVKHI0U644AoYROdJbSzCWdZHzw6D4zDdUD0v6VZP9OaFnyC4MfMTcLUhjm6s_dGsD9-hj4UjjslT4t3otV_-ot9pY0FrPGf24PzGYbH1dCgg47a5Jo4dMAjxAXwwwZsnggi3VQBz6MWb1W6mmlh-dfPYM8xR9OoOjm0DtGz0047BGnvceg";//this.lottabyteClient.preauth();
            this.bearerToken = this.lottabyteClient.preauth();
            this.userDetails = jwtHelper.getUserDetail(bearerToken);
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
        }
        activeTaskSchedules = new HashMap<>();
    }

    public String getActiveTaskSchedules(UserDetails userDetails) throws LottabyteException {
        List<JSONObject> activeTaskSchedulesJsonList = new ArrayList<>();
        for (TaskSchedule ts : activeTaskSchedules.values()) {
            TaskRun taskRun = taskRunService.getTaskRunByTaskScheduleId(ts.getId(), userDetails);
            if(taskRun != null){
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                ObjectWriter ow = objectMapper.writer().withDefaultPrettyPrinter();
                String json;
                try {
                    json = ow.writeValueAsString(ts);
                } catch (JsonProcessingException e) {
                    throw new LottabyteException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
                }

                JSONObject jSONObject = new JSONObject(json);
                jSONObject.put("taskStart", taskRun.getEntity().getTaskStart());
                jSONObject.put("taskState", taskRun.getEntity().getTaskState());
                jSONObject.put("taskEnd", taskRun.getEntity().getTaskEnd());

                activeTaskSchedulesJsonList.add(jSONObject);
            }
        }
        return activeTaskSchedulesJsonList.toString();
    }

    public boolean fillingActiveTaskSchedules() throws LottabyteException {
        log.info("fillingActiveTaskSchedules");
        boolean changed = false;
        try {
            System.out.println(userDetails);
            List<Task> externalTaskList = taskService.getAllTasks(userDetails);

            if(activeTaskSchedules.isEmpty()) {
                for(Task externalTask : externalTaskList) {
                    for (TaskSchedule ts : externalTask.getEntity().getSchedules()) {
                        if (ts.getEntity().getEnabled()) {
                            activeTaskSchedules.put(ts.getId(), ts);
                            changed = true;
                        }
                    }
                }
            } else {
                for(Task externalTask : externalTaskList) {
                    for (TaskSchedule ts : externalTask.getEntity().getSchedules()) {
                        if (!ts.getEntity().getEnabled()) {
                            activeTaskSchedules.remove(externalTask.getId());
                            changed = true;
                        } else {
                            TaskSchedule localTaskSchedule = activeTaskSchedules.get(ts.getId());
                            if (localTaskSchedule == null) {
                                activeTaskSchedules.put(ts.getId(), ts);
                                changed = true;
                            } else {
                                if (!localTaskSchedule.equals(ts) || localTaskSchedule.getModifiedAt().isBefore(ts.getModifiedAt())) {
                                    activeTaskSchedules.put(ts.getId(), ts);
                                    changed = true;
                                }
                            }
                        }
                    }
                }
            }
            for(Task externalTask : externalTaskList){
                for (TaskSchedule ts : externalTask.getEntity().getSchedules()) {
                    List<TaskRun> taskRunListToCheck = taskRunService.getTaskRunListByTaskScheduleId(ts.getId(), userDetails);
                    if (!taskRunListToCheck.isEmpty())
                        activeTaskSchedules.remove(ts.getId());
                }
            }
        } catch (LottabyteException e) {
            throw new LottabyteException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }

        return changed;
    }

    public Trigger getTrigger(TaskSchedule taskSchedule) throws LottabyteException {
        Trigger trigger = null;
        SimpleDateFormat sdf = null;

        try {
            final ObjectNode node = new ObjectMapper().readValue(taskSchedule.getEntity().getScheduleParams(), ObjectNode.class);
            Date currentDate = new Date();

            switch(taskSchedule.getEntity().getScheduleType())
            {
                case ONCE:
                    log.info(node.toString());
                    if (node.has("datetime")) {
                        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        Date date = sdf.parse(String.valueOf(node.get("datetime")).replaceAll("\"", ""));
                        log.info("date: " + date);
                        log.info("currentDate: " + currentDate);
                        log.info(String.valueOf(date.before(currentDate)));
                        if(date.before(currentDate)) {
                            activeTaskSchedules.remove(taskSchedule.getId());
                            taskService.updateTaskScheduleEnabled(taskSchedule.getId(), userDetails);
                            log.error(Message.format(Message.LBE00066.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), node, ONCE));
                            return null;
                        }

                        trigger = TriggerBuilder.newTrigger()
                                .withIdentity(taskSchedule.getId(), "ONCE Trigger")
                                .startAt(date)
                                .withSchedule(simpleSchedule())
                                .build();

                        activeTaskSchedules.remove(taskSchedule.getId());
                        taskService.updateTaskScheduleEnabled(taskSchedule.getId(), userDetails);
                        return trigger;
                    }else{
                        throw new LottabyteException(Message.LBE00062, userDetails.getLanguage(), node, ONCE);
                    }
                case DAILY:
                    if (node.has("datetime")) {
                        String[] time = String.valueOf(node.get("datetime")).substring(12).replaceAll("\"", "").split(":");
                        int hours = Integer.parseInt(time[0]);
                        int minutes = Integer.parseInt(time[1]);
                        int seconds = 0;//Integer.parseInt(time[2]);
                        String cronSchedule = seconds + " " + minutes + " " + hours + " 1/1 * ? *";

                        trigger = TriggerBuilder.newTrigger()
                                .withIdentity(taskSchedule.getId(), "DAILY Trigger")
                                .withSchedule(CronScheduleBuilder.cronSchedule(cronSchedule))
                                .build();
                        return trigger;
                    }else{
                        throw new LottabyteException(Message.LBE00062, userDetails.getLanguage(), node, DAILY);
                    }
                case WEEKLY:
                    if (node.has("datetime")) {
                        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        Date date = sdf.parse(String.valueOf(node.get("datetime")).replaceAll("\"", ""));
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(date);

                        int targetDay = cal.get(Calendar.DAY_OF_WEEK); //Integer.parseInt(String.valueOf(node.get("dow")).replaceAll("\"", ""));
                        //String[] time = String.valueOf(node.get("time")).replaceAll("\"", "").split(":");
                        int hours = cal.get(Calendar.HOUR_OF_DAY); //Integer.parseInt(time[0]);
                        int minutes = cal.get(Calendar.MINUTE); //Integer.parseInt(time[1]);
                        int seconds = 0; //Integer.parseInt(time[2]);
                        String cronSchedule = seconds + " " + minutes + " " + hours + " ? * " + Weekday.fromString(Integer.toString(targetDay)) + " *";

                        trigger = TriggerBuilder.newTrigger()
                                .withIdentity(taskSchedule.getId(), "WEEKLY Trigger")
                                .withSchedule(CronScheduleBuilder.cronSchedule(cronSchedule))
                                .build();
                        return trigger;
                    }else{
                        throw new LottabyteException(Message.LBE00062, userDetails.getLanguage(), node, WEEKLY);
                    }
                case MONTHLY:
                    if (node.has("datetime")) {
                        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        Date date = sdf.parse(String.valueOf(node.get("datetime")).replaceAll("\"", ""));
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(date);

                        int targetDay = cal.get(Calendar.DAY_OF_MONTH);
                        //String[] time = String.valueOf(node.get("time")).replaceAll("\"", "").split(":");
                        int hours = cal.get(Calendar.HOUR_OF_DAY); //Integer.parseInt(time[0]);
                        int minutes = cal.get(Calendar.MINUTE); //Integer.parseInt(time[1]);
                        int seconds = 0; //Integer.parseInt(time[2]);
                        String cronSchedule = seconds + " " + minutes + " " + hours + " " + targetDay + " 1/1 ? *";

                        trigger = TriggerBuilder.newTrigger()
                                .withIdentity(taskSchedule.getId(), "MONTHLY Trigger")
                                .withSchedule(CronScheduleBuilder.cronSchedule(cronSchedule))
                                .build();
                        return trigger;
                    }else{
                        throw new LottabyteException(Message.LBE00062, userDetails.getLanguage(), node, MONTHLY);
                    }
                case CRON:
                    if (node.has("cron_schedule")) {
                        String cronSchedule = String.valueOf(node.get("cron_schedule")).replaceAll("\"", "");
                        isValidExpression(cronSchedule);

                        trigger = TriggerBuilder.newTrigger()
                                .withIdentity(taskSchedule.getId(), "CRON Trigger")
                                .withSchedule(CronScheduleBuilder.cronSchedule(cronSchedule))
                                .build();
                        return trigger;
                    }else{
                        throw new LottabyteException(Message.LBE00062, userDetails.getLanguage(), node, CRON);
                    }
                default:
                    throw new LottabyteException(Message.LBE00063, userDetails.getLanguage(), taskSchedule.getEntity().getScheduleType());
            }
        } catch (ParseException | JsonProcessingException e) {
            throw new LottabyteException(e.getMessage(), e);
        }
    }

    public void completingTasks() throws LottabyteException, SchedulerException {
        boolean changed = fillingActiveTaskSchedules();

        log.info("Scheduler activeTasks = " + activeTaskSchedules.size());

        for(TaskSchedule taskSchedule : activeTaskSchedules.values()) {
            log.info("Scheduler start task schedule " + taskSchedule.getId());


            Trigger trigger = getTrigger(taskSchedule);

            JobDataMap data = new JobDataMap();
            data.put("lottabyteClient", lottabyteClient);
            data.put("taskId", taskSchedule.getId());
            JobBuilder jobBuilder = JobBuilder.newJob(TaskJob.class);
            JobDetail jobDetail = jobBuilder.usingJobData("example", "com.javacodegeeks.quartz.QuartzSchedulerExample")
                    .usingJobData(data)
                    .withIdentity(taskSchedule.getId(), "TaskGroup")
                    .build();


            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Scheduler scheduler = schedulerFactory.getScheduler();
            if (changed)
                scheduler.deleteJob(jobDetail.getKey());
            scheduler.start();

            try {
                log.info("scheduleJob " + jobDetail.getJobDataMap().get("taskId"));
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (SchedulerException e) {
                log.error(e.getMessage());
                break;
            }

        }
    }
}
