package ru.bssg.lottabyte.core.connector;

public enum ConnectorType {
    JDBC("JDBC Table Connector"),
    S3("Excel File in S3 Storage Connector"),
    REST_API("Generic REST API Connector");

    private String text;

    ConnectorType(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public static ConnectorType fromString(String text) {
        for (ConnectorType b : ConnectorType.values()) {
            if (b.text.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
}
