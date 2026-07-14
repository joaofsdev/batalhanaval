package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class PlayerAlreadyInGameException extends DomainException {

    public PlayerAlreadyInGameException() {
        super("Jogador já está em uma partida ativa", "PLAYER_ALREADY_IN_GAME", HttpStatus.CONFLICT);
    }
}
