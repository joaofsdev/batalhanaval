package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class GameNotInPlacingException extends DomainException {

    public GameNotInPlacingException() {
        super("A partida não está na fase de posicionamento", "GAME_NOT_IN_PLACING", HttpStatus.CONFLICT);
    }
}
