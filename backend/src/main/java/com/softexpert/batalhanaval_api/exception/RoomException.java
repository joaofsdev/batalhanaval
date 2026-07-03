package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class RoomException extends DomainException {

    public RoomException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
    }

    public RoomException(String message, String errorCode, HttpStatus httpStatus) {
        super(message, errorCode, httpStatus);
    }
}
