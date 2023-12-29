package ru.bssg.lottabyte.core.model.ca;

public enum AttributeType {
    Reference("Reference"),
    String("String"),
    Date("Date"),
    Enumerated("Enumerated"),
    Numeric("Numeric");

    private String text;

    AttributeType(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public static AttributeType fromString(String text) {
        for (AttributeType b : AttributeType.values()) {
            if (b.text.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
}