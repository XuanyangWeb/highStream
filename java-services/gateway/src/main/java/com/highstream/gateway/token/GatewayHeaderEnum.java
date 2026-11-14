package com.highstream.gateway.token;

public enum GatewayHeaderEnum {
    X_USER_ID("X_User_Id"),

    X_USER_ROLE("X-User-Role"),

    X_TRACE_ID("X-Trace-Id");

    private final String header;

    GatewayHeaderEnum(String header) {
        this.header = header;
    }
    public String getHeader() {
        return header;
    }
}
