package ru.bssg.lottabyte.core.model.entitySample;

public enum EntitySampleType {
    json("json"),
    xml("xml"),
    csv("csv"),
    text("text"),
    table("table"),
    unknown("unknown");

    private String val;

    EntitySampleType(String val) {
        this.val = val;
    }

    public String getVal() {
        return this.val;
    }

    public static EntitySampleType fromString(String val) {
        for (EntitySampleType b : EntitySampleType.values()) {
            if (b.val.equalsIgnoreCase(val)) {
                return b;
            }
        }
        return null;
    }
}
