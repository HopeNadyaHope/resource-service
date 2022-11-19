package com.epam.microservices.service.exception;

import java.text.MessageFormat;

public class NoBucketForStorageTypeException extends RuntimeException {

    private static final String ERROR_MESSAGE_PATTERN = "No bucket for storage type {0}";

    public NoBucketForStorageTypeException(String storageType) {
        super(MessageFormat.format(ERROR_MESSAGE_PATTERN, storageType));

    }
}
