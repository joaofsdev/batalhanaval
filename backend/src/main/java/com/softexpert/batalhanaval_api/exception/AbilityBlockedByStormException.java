package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class AbilityBlockedByStormException extends DomainException {

    public AbilityBlockedByStormException() {
        super("Abilities are blocked during storm turns", "ABILITY_BLOCKED_BY_STORM", HttpStatus.CONFLICT);
    }
}
