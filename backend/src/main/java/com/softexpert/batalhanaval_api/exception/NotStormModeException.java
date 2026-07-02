package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class NotStormModeException extends DomainException {

    public NotStormModeException() {
        super("This action is only available in Storm mode", "NOT_STORM_MODE", HttpStatus.BAD_REQUEST);
    }
}
