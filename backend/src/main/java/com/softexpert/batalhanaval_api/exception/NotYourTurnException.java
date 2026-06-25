package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class NotYourTurnException extends DomainException {

    public NotYourTurnException() {
        super("It's not your turn to fire", "NOT_YOUR_TURN", HttpStatus.CONFLICT);
    }
}
