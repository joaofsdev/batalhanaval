package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class AccountDisabledException extends DomainException {

    public AccountDisabledException() {
        super("Sua conta está suspensa ou banida", "ACCOUNT_DISABLED", HttpStatus.FORBIDDEN);
    }
}
