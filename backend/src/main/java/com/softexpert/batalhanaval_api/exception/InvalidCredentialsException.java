package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Invalid username or password", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
    }
}
