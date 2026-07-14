package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class NotYourTurnException extends DomainException {

    public NotYourTurnException() {
        super("Não é o seu turno de atacar", "NOT_YOUR_TURN", HttpStatus.CONFLICT);
    }
}
