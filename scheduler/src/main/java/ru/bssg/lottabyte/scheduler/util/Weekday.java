package ru.bssg.lottabyte.scheduler.util;

public enum Weekday {
    MON("2"),
    TUE("3"),
    WED("4"),
    THU("5"),
    FRI("6"),
    SAT("7"),
    SUN("1");

    private final String text;

    Weekday(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public static Weekday fromString(String text) {
        for (Weekday b : Weekday.values()) {
            if (b.text.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
}