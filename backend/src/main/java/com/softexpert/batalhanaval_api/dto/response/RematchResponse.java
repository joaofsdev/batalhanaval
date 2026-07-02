package com.softexpert.batalhanaval_api.dto.response;

import java.util.UUID;

public record RematchResponse(
    RematchStatus status,
    UUID gameId
) {
    public enum RematchStatus {
        WAITING,
        MATCHED
    }
}
