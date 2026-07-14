package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyTakenException extends DomainException {

    public UsernameAlreadyTakenException() {
        super("Este nome de usuário já está em uso", "USERNAME_TAKEN", HttpStatus.CONFLICT);
    }
}
