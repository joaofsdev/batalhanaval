package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class CellAlreadyAttackedException extends DomainException {

    public CellAlreadyAttackedException() {
        super("Esta posição já foi atacada", "CELL_ALREADY_ATTACKED", HttpStatus.CONFLICT);
    }
}
