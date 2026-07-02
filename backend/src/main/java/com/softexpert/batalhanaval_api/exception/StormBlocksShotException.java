package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class StormBlocksShotException extends DomainException {

    public StormBlocksShotException(String message) {
        super(message, "STORM_BLOCKS_SHOT", HttpStatus.CONFLICT);
    }
}
