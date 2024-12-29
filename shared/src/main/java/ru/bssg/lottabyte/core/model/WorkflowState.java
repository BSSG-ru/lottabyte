package ru.bssg.lottabyte.core.model;

public enum WorkflowState {
    NOT_STARTED,
    MARKED_FOR_REMOVAL,
    MARKED_FOR_ARCHIVE,
    MARKED_FOR_RESTORE,
    READY_FOR_PUBLISH;

    private WorkflowState() {

    }

}
