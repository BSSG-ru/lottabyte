package ru.bssg.lottabyte.core.model.util;

public enum RestApiAuthMethod {
    none("none"),
    basic("basic"),
    bearer_token("bearer token");

    private final String val;

    RestApiAuthMethod(String val) {
        this.val = val;
    }

    public String getVal() {
        return this.val;
    }

    public static RestApiAuthMethod fromString(String val) {
        for (RestApiAuthMethod b : RestApiAuthMethod.values()) {
            if (b.val.equalsIgnoreCase(val)) {
                return b;
            }
        }
        return null;
    }
}
