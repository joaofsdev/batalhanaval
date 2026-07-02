package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class InvalidAbilityTypeException extends DomainException {

    public InvalidAbilityTypeException() {
        super("The ability type does not match the player's assigned ability", "INVALID_ABILITY_TYPE", HttpStatus.BAD_REQUEST);
    }
}
