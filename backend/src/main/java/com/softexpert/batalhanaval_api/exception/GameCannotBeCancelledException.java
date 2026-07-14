package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class GameCannotBeCancelledException extends DomainException {

    public GameCannotBeCancelledException() {
        super("A partida só pode ser cancelada enquanto aguarda um oponente", "GAME_CANNOT_BE_CANCELLED", HttpStatus.CONFLICT);
    }
}
