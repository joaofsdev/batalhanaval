package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class GameNotFoundException extends DomainException {

    public GameNotFoundException() {
        super("Partida não encontrada", "GAME_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
