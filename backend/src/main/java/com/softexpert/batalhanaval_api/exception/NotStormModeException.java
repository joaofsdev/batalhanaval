package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class NotStormModeException extends DomainException {

    public NotStormModeException() {
        super("Esta ação está disponível apenas no modo Tempestade", "NOT_STORM_MODE", HttpStatus.BAD_REQUEST);
    }
}
