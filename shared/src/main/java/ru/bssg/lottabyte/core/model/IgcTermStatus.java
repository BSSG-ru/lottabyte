package ru.bssg.lottabyte.core.model;

public enum IgcTermStatus {
    CANDIDATE(ManagedEntityState.DRAFT),
    ACCEPTED(ManagedEntityState.PUBLISHED),
    STANDARD(ManagedEntityState.PUBLISHED),
    DEPRECATED(ManagedEntityState.ARCHIVED);

    private ManagedEntityState correspondingStatus;

    private IgcTermStatus(ManagedEntityState correspondingStatus) {
        this.correspondingStatus = correspondingStatus;
    }

    public ManagedEntityState getCorrespondingState() {
        return this.correspondingStatus;
    }
}
