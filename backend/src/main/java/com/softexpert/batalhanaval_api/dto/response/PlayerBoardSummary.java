package com.softexpert.batalhanaval_api.dto.response;

import java.util.UUID;

public record PlayerBoardSummary(
    UUID playerId,
    String username,
    BoardResponse board
) {}
