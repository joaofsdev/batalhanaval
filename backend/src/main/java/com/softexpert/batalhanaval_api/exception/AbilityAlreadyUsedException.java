package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class AbilityAlreadyUsedException extends DomainException {

    public AbilityAlreadyUsedException() {
        super("A habilidade já foi utilizada", "ABILITY_ALREADY_USED", HttpStatus.CONFLICT);
    }
}
