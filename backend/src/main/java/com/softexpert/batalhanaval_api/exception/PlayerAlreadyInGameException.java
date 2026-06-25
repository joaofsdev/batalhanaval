package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class PlayerAlreadyInGameException extends DomainException {

    public PlayerAlreadyInGameException() {
        super("Player is already in an active game", "PLAYER_ALREADY_IN_GAME", HttpStatus.CONFLICT);
    }
}
