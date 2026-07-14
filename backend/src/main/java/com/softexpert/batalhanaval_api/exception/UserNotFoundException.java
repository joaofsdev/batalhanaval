package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends DomainException {

    public UserNotFoundException() {
        super("Usuário não encontrado", "USER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
