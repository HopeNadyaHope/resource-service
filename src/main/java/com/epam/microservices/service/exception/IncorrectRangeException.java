package com.epam.microservices.service.exception;

import java.text.MessageFormat;

public class IncorrectRangeException extends RuntimeException {
    private static final String ERROR_MESSAGE_PATTERN = "Incorrect range {0} - {1}";
    private final int start;
    private final int end;

    public IncorrectRangeException(int start, int end) {
        super(MessageFormat.format(ERROR_MESSAGE_PATTERN, start, end));
        this.start = start;
        this.end = end;
    }
}
