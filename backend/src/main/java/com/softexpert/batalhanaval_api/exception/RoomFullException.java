package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class RoomFullException extends DomainException {

    public RoomFullException() {
        super("Esta sala já está cheia ou a partida já começou.", "ROOM_FULL", HttpStatus.CONFLICT);
    }
}
