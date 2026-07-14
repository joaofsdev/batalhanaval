package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class InvalidAbilityTypeException extends DomainException {

    public InvalidAbilityTypeException() {
        super("O tipo de habilidade não corresponde à habilidade atribuída ao jogador", "INVALID_ABILITY_TYPE", HttpStatus.BAD_REQUEST);
    }
}
