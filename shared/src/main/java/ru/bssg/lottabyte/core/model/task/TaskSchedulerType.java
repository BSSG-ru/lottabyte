package ru.bssg.lottabyte.core.model.task;

public enum TaskSchedulerType {
    ONCE("ONCE"),
    DAILY("DAILY"),
    WEEKLY("WEEKLY"),
    MONTHLY("MONTHLY"),
    CRON("CRON");

    private String text;

    TaskSchedulerType(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public static TaskSchedulerType fromString(String text) {
        for (TaskSchedulerType b : TaskSchedulerType.values()) {
            if (b.text.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
}
