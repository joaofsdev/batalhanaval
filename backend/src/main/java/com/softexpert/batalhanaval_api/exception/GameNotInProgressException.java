package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class GameNotInProgressException extends DomainException {

    public GameNotInProgressException() {
        super("A partida não está em andamento", "GAME_NOT_IN_PROGRESS", HttpStatus.CONFLICT);
    }
}
