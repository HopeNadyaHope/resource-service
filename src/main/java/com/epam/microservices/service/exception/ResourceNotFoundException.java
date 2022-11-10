package com.epam.microservices.service.exception;

import java.text.MessageFormat;

public class ResourceNotFoundException extends RuntimeException {

    private static final String ERROR_MESSAGE_PATTERN = "Resource with id {0} not found";

    public ResourceNotFoundException(int resourceId) {
        super(MessageFormat.format(ERROR_MESSAGE_PATTERN, resourceId));
    }
}
