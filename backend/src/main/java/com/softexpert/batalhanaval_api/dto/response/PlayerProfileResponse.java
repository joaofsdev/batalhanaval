package com.softexpert.batalhanaval_api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlayerProfileResponse(
    UUID id,
    String username,
    long totalGames,
    long wins,
    long losses,
    double winRate,
    int rank,
    Instant memberSince,
    long totalShots,
    long shotsHit,
    long shipsSunk,
    double accuracy,
    List<GameHistoryEntry> recentGames
) {}
