package com.epam.microservices.service.exception;

public class UnableToSaveFileException extends RuntimeException{
    private static final String UNABLE_TO_SAVE_FILE_ERROR_MESSAGE = "Unable to save file to storage";

    public UnableToSaveFileException(){
        super(UNABLE_TO_SAVE_FILE_ERROR_MESSAGE);
    }
}
