package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class BoardAlreadyReadyException extends DomainException {

    public BoardAlreadyReadyException() {
        super("Fleet has already been placed", "BOARD_ALREADY_READY", HttpStatus.CONFLICT);
    }
}
