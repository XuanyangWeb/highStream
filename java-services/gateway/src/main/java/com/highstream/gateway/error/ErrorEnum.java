package com.highstream.gateway.error;

public enum ErrorEnum {

    AUTHOR_ERROR("An exception occurred during authentication.");

    private final String errorPrefix;


    ErrorEnum(String errorPrefix) {
        this.errorPrefix = errorPrefix;
    }

}
