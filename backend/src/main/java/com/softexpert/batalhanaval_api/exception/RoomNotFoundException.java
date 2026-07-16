package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class RoomNotFoundException extends DomainException {

    public RoomNotFoundException() {
        super("Sala não encontrada com este token.", "ROOM_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
