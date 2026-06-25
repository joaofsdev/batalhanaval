package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class CellAlreadyAttackedException extends DomainException {

    public CellAlreadyAttackedException() {
        super("This cell has already been attacked", "CELL_ALREADY_ATTACKED", HttpStatus.CONFLICT);
    }
}
