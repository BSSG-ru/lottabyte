package ru.bssg.lottabyte.core.model;

public enum ManagedEntityState {
    DRAFT(IgcTermStatus.CANDIDATE),
    PUBLISHED(IgcTermStatus.STANDARD),
    DRAFT_HISTORY(IgcTermStatus.DEPRECATED),
    PUBLISHED_HISTORY(IgcTermStatus.DEPRECATED),
    DELETED(IgcTermStatus.DEPRECATED),
    ARCHIVED(IgcTermStatus.DEPRECATED);

    IgcTermStatus igcStatus;

    public IgcTermStatus getIgcStatus() {
        return this.igcStatus;
    }

    private ManagedEntityState(final IgcTermStatus igcStatus) {
        this.igcStatus = igcStatus;
    }
}
