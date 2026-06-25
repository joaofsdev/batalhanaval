package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class GameNotFoundException extends DomainException {

    public GameNotFoundException() {
        super("Game not found", "GAME_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
