package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class NotGameParticipantException extends DomainException {

    public NotGameParticipantException() {
        super("Você não é participante desta partida", "NOT_GAME_PARTICIPANT", HttpStatus.FORBIDDEN);
    }
}
