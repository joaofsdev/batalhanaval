package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class GameNotInPlacingException extends DomainException {

    public GameNotInPlacingException() {
        super("Game is not in placing phase", "GAME_NOT_IN_PLACING", HttpStatus.CONFLICT);
    }
}
