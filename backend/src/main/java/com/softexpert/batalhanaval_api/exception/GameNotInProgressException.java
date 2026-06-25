package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class GameNotInProgressException extends DomainException {

    public GameNotInProgressException() {
        super("Game is not in progress", "GAME_NOT_IN_PROGRESS", HttpStatus.CONFLICT);
    }
}
