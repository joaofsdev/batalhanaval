package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyTakenException extends DomainException {

    public EmailAlreadyTakenException() {
        super("Email is already taken", "EMAIL_TAKEN", HttpStatus.CONFLICT);
    }
}
