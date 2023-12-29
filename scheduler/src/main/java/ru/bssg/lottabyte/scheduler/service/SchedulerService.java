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
import ru.bssg.lottabyte.core.model.taskrun.TaskRun;
import ru.bssg.lottabyte.core.usermanagement.model.UserDetails;
import ru.bssg.lottabyte.core.usermanagement.security.JwtHelper;
import ru.bssg.lottabyte.scheduler.client.LottabyteClient;
import ru.bssg.lottabyte.scheduler.job.TaskJob;
import ru.bssg.lottabyte.scheduler.util.Weekday;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
    private final Map<String, Task> activeTasks;

    @Autowired
    public SchedulerService(LottabyteClient lottabyteClient, TaskService taskService, JwtHelper jwtHelper, TaskRunService taskRunService) {
        this.lottabyteClient = lottabyteClient;
        this.taskService = taskService;
        this.jwtHelper = jwtHelper;
        this.taskRunService = taskRunService;
        try {
            this.bearerToken = this.lottabyteClient.preauth();
            this.userDetails = jwtHelper.getUserDetail(bearerToken);
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
        }
        activeTasks = new HashMap<>();
    }

    public String getActiveTasks(UserDetails userDetails) throws LottabyteException {
        List<JSONObject> activeTaskJsonList = new ArrayList<>();
        for (Task task : activeTasks.values()) {
            TaskRun taskRun = taskRunService.getTaskRunByTaskId(task.getId(), userDetails);
            if(taskRun != null){
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                ObjectWriter ow = objectMapper.writer().withDefaultPrettyPrinter();
                String json;
                try {
                    json = ow.writeValueAsString(task);
                } catch (JsonProcessingException e) {
                    throw new LottabyteException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
                }

                JSONObject jSONObject = new JSONObject(json);
                jSONObject.put("taskStart", taskRun.getEntity().getTaskStart());
                jSONObject.put("taskState", taskRun.getEntity().getTaskState());
                jSONObject.put("taskEnd", taskRun.getEntity().getTaskEnd());

                activeTaskJsonList.add(jSONObject);
            }
        }
        return activeTaskJsonList.toString();
    }

    public boolean fillingActiveTasks() throws LottabyteException {
        boolean changed = false;
        try {
            System.out.println(userDetails);
            List<Task> externalTaskList = taskService.getAllTasks(userDetails);

            if(activeTasks.isEmpty()){
                for(Task externalTask : externalTaskList){
                    if (externalTask.getEntity().getEnabled()) {
                        activeTasks.put(externalTask.getId(), externalTask);
                        changed = true;
                    }
                }
            }else{
                for(Task externalTask : externalTaskList) {
                    if (!externalTask.getEntity().getEnabled()) {
                        activeTasks.remove(externalTask.getId());
                        changed = true;
                    } else {
                        Task localTask = activeTasks.get(externalTask.getId());
                        if (localTask == null) {
                            activeTasks.put(externalTask.getId(), externalTask);
                            changed = true;
                        } else {
                            if (!localTask.equals(externalTask) || localTask.getModifiedAt().isBefore(externalTask.getModifiedAt())) {
                                activeTasks.put(externalTask.getId(), externalTask);
                                changed = true;
                            }
                        }
                    }
                }
            }
            for(Task externalTask : externalTaskList){
                List<TaskRun> taskRunListToCheck = taskRunService.getTaskRunListByTaskId(externalTask.getId(), userDetails);
                if(!taskRunListToCheck.isEmpty())
                    activeTasks.remove(externalTask.getId());
            }
        } catch (LottabyteException e) {
            throw new LottabyteException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }

        return changed;
    }

    public Trigger getTrigger(Task task) throws LottabyteException {
        Trigger trigger = null;
        SimpleDateFormat sdf = null;

        try {
            final ObjectNode node = new ObjectMapper().readValue(task.getEntity().getScheduleParams(), ObjectNode.class);
            Date currentDate = new Date();

            switch(task.getEntity().getScheduleType())
            {
                case ONCE:
                    log.info(node.toString());
                    if (node.has("datetime")) {
                        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date date = sdf.parse(String.valueOf(node.get("datetime")).replaceAll("\"", ""));
                        log.info("date: " + date);
                        log.info("currentDate: " + currentDate);
                        log.info(String.valueOf(date.before(currentDate)));
                        if(date.before(currentDate)) {
                            activeTasks.remove(task.getId());
                            taskService.updateTaskEnabled(task.getId(), userDetails);
                            log.error(Message.format(Message.LBE00066.getText(userDetails.getLanguage().name()), userDetails.getLanguage().name(), node, ONCE));
                            return null;
                        }

                        trigger = TriggerBuilder.newTrigger()
                                .withIdentity(task.getId(), "ONCE Trigger")
                                .startAt(date)
                                .withSchedule(simpleSchedule())
                                .build();

                        activeTasks.remove(task.getId());
                        taskService.updateTaskEnabled(task.getId(), userDetails);
                        return trigger;
                    }else{
                        throw new LottabyteException(Message.LBE00062, userDetails.getLanguage(), node, ONCE);
                    }
                case DAILY:
                    if (node.has("time")) {
                        String[] time = String.valueOf(node.get("time")).replaceAll("\"", "").split(":");
                        int hours = Integer.parseInt(time[0]);
                        int minutes = Integer.parseInt(time[1]);
                        int seconds = Integer.parseInt(time[2]);
                        String cronSchedule = seconds + " " + minutes + " " + hours + " 1/1 * ? *";

                        trigger = TriggerBuilder.newTrigger()
                                .withIdentity(task.getId(), "DAILY Trigger")
                                .withSchedule(CronScheduleBuilder.cronSchedule(cronSchedule))
                                .build();
                        return trigger;
                    }else{
                        throw new LottabyteException(Message.LBE00062, userDetails.getLanguage(), node, DAILY);
                    }
                case WEEKLY:
                    if (node.has("dow") && node.has("time")) {
                        int targetDay = Integer.parseInt(String.valueOf(node.get("dow")).replaceAll("\"", ""));
                        String[] time = String.valueOf(node.get("time")).replaceAll("\"", "").split(":");
                        int hours = Integer.parseInt(time[0]);
                        int minutes = Integer.parseInt(time[1]);
                        int seconds = Integer.parseInt(time[2]);
                        String cronSchedule = seconds + " " + minutes + " " + hours + " ? * " + Weekday.fromString(Integer.toString(targetDay)) + " *";

                        trigger = TriggerBuilder.newTrigger()
                                .withIdentity(task.getId(), "WEEKLY Trigger")
                                .withSchedule(CronScheduleBuilder.cronSchedule(cronSchedule))
                                .build();
                        return trigger;
                    }else{
                        throw new LottabyteException(Message.LBE00062, userDetails.getLanguage(), node, WEEKLY);
                    }
                case MONTHLY:
                    if (node.has("dom") && node.has("time")) {
                        int targetDay = Integer.parseInt(String.valueOf(node.get("dom")).replaceAll("\"", ""));
                        String[] time = String.valueOf(node.get("time")).replaceAll("\"", "").split(":");
                        int hours = Integer.parseInt(time[0]);
                        int minutes = Integer.parseInt(time[1]);
                        int seconds = Integer.parseInt(time[2]);
                        String cronSchedule = seconds + " " + minutes + " " + hours + " " + targetDay + " 1/1 ? *";

                        trigger = TriggerBuilder.newTrigger()
                                .withIdentity(task.getId(), "MONTHLY Trigger")
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
                                .withIdentity(task.getId(), "CRON Trigger")
                                .withSchedule(CronScheduleBuilder.cronSchedule(cronSchedule))
                                .build();
                        return trigger;
                    }else{
                        throw new LottabyteException(Message.LBE00062, userDetails.getLanguage(), node, CRON);
                    }
                default:
                    throw new LottabyteException(Message.LBE00063, userDetails.getLanguage(), task.getEntity().getScheduleType());
            }
        } catch (ParseException | JsonProcessingException e) {
            throw new LottabyteException(e.getMessage(), e);
        }
    }

    public void completingTasks() throws LottabyteException, SchedulerException {
        boolean changed = fillingActiveTasks();

         for(Task task : activeTasks.values()){
            Trigger trigger = getTrigger(task);

            JobDataMap data = new JobDataMap();
            data.put("lottabyteClient", lottabyteClient);
            data.put("taskId", task.getId());
            JobBuilder jobBuilder = JobBuilder.newJob(TaskJob.class);
            JobDetail jobDetail = jobBuilder.usingJobData("example", "com.javacodegeeks.quartz.QuartzSchedulerExample")
                    .usingJobData(data)
                    .withIdentity(task.getId(), "TaskGroup")
                    .build();


            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Scheduler scheduler = schedulerFactory.getScheduler();
            if(changed)
                scheduler.deleteJob(jobDetail.getKey());
            scheduler.start();

            try{
                scheduler.scheduleJob(jobDetail, trigger);
            }catch (SchedulerException e){
                log.error(e.getMessage());
                break;
            }
        }
    }
}
