package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class AccountDisabledException extends DomainException {

    public AccountDisabledException() {
        super("Your account is suspended or banned", "ACCOUNT_DISABLED", HttpStatus.FORBIDDEN);
    }
}
