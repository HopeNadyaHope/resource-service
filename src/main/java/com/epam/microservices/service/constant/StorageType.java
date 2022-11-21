package com.epam.microservices.service.constant;

public enum StorageType {
    PERMANENT("permanent"),
    STAGING("staging");

    private final String value;

    StorageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
