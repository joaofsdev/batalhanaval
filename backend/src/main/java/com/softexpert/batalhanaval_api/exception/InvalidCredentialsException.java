package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Usuário ou senha inválidos", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
    }
}
