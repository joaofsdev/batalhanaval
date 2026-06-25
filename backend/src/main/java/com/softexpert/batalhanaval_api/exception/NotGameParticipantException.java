package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class NotGameParticipantException extends DomainException {

    public NotGameParticipantException() {
        super("You are not a participant of this game", "NOT_GAME_PARTICIPANT", HttpStatus.FORBIDDEN);
    }
}
