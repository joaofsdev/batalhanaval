package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class InvalidShipPlacementException extends DomainException {

    public InvalidShipPlacementException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
    }
}
