package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyTakenException extends DomainException {

    public UsernameAlreadyTakenException() {
        super("Username is already taken", "USERNAME_TAKEN", HttpStatus.CONFLICT);
    }
}
