package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class AbilityBlockedByStormException extends DomainException {

    public AbilityBlockedByStormException() {
        super("Habilidades estão bloqueadas durante turnos de tempestade", "ABILITY_BLOCKED_BY_STORM", HttpStatus.CONFLICT);
    }
}
