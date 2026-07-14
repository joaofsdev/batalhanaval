package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyTakenException extends DomainException {

    public EmailAlreadyTakenException() {
        super("Este email já está em uso", "EMAIL_TAKEN", HttpStatus.CONFLICT);
    }
}
