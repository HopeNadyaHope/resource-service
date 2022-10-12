package com.epam.microservices.service.exception;

import java.text.MessageFormat;

public class ResourceCantBeReachedException extends RuntimeException {
    private static final String ERROR_MESSAGE_PATTERN = "Resource {0} can't be reached";
    private final int resourceId;

    public ResourceCantBeReachedException(int resourceId) {
        super(MessageFormat.format(ERROR_MESSAGE_PATTERN, resourceId));
        this.resourceId = resourceId;
    }
}
