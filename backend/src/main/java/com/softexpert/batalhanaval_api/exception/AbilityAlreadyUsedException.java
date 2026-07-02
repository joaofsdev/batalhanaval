package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class AbilityAlreadyUsedException extends DomainException {

    public AbilityAlreadyUsedException() {
        super("Ability has already been used", "ABILITY_ALREADY_USED", HttpStatus.CONFLICT);
    }
}
