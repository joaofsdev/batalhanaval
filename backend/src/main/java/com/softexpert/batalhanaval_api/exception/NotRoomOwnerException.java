package com.softexpert.batalhanaval_api.exception;

import org.springframework.http.HttpStatus;

public class NotRoomOwnerException extends DomainException {

    public NotRoomOwnerException() {
        super("Apenas o criador da sala pode realizar esta ação.", "NOT_ROOM_OWNER", HttpStatus.FORBIDDEN);
    }
}
