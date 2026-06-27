package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class GameCannotBeCancelledException extends DomainException {

    public GameCannotBeCancelledException() {
        super("Game can only be cancelled while waiting for an opponent", "GAME_CANNOT_BE_CANCELLED", HttpStatus.CONFLICT);
    }
}
