package com.epam.microservices.controller.exception;

import com.epam.microservices.service.exception.IncorrectRangeException;
import com.epam.microservices.service.exception.ResourceCantBeReachedException;
import com.epam.microservices.service.exception.UnableToSaveFileException;
import org.hibernate.ObjectNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErrorHandler {
    private static final String ERROR_PROCESSING_REQUEST = "There was an error processing the request";

    @ExceptionHandler(ObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public @ResponseBody ApiError objectNotFoundException(ObjectNotFoundException e) {
        return new ApiError(HttpStatus.NOT_FOUND,
                e.getMessage());
    }

    @ExceptionHandler(ResourceCantBeReachedException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public @ResponseBody ApiError resourceCantBeReachedException(ResourceCantBeReachedException e) {
        return new ApiError(HttpStatus.NOT_FOUND,
                e.getMessage());
    }

    @ExceptionHandler(IncorrectRangeException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public @ResponseBody ApiError incorrectRangeException(IncorrectRangeException e) {
        return new ApiError(HttpStatus.CONFLICT,
                e.getMessage());
    }

    @ExceptionHandler(UnableToSaveFileException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public @ResponseBody ApiError unableToSaveFileException(UnableToSaveFileException e) {
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR,
                e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError otherException() {
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR,
                ERROR_PROCESSING_REQUEST);
    }

}
