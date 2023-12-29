package ru.bssg.lottabyte.scheduler.util;

public enum Weekday {
    MON("1"),
    TUE("2"),
    WED("3"),
    THU("4"),
    FRI("5"),
    SAT("6"),
    SUN("7");

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