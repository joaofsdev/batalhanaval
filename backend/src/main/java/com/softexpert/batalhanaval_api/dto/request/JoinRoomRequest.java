package com.softexpert.batalhanaval_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record JoinRoomRequest(
    @NotBlank(message = "token is required")
    String token
) {
}
