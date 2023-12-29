package ru.bssg.lottabyte.core.model.task;

public enum TaskState {

    STARTED("STARTED"),
    RUNNING("RUNNING"),
    FINISHED("FINISHED"),
    FAILED("FAILED");

    private String text;

    TaskState(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public static TaskState fromString(String text) {
        for (TaskState b : TaskState.values()) {
            if (b.text.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }

}
