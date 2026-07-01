package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.GameStatus;

import java.time.Instant;
import java.util.UUID;

public record GameHistoryEntry(
    UUID id,
    String opponentUsername,
    GameStatus status,
    boolean won,
    long durationSeconds,
    Instant playedAt
) {}
