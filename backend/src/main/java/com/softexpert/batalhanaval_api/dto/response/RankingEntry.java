package com.softexpert.batalhanaval_api.dto.response;

import java.util.UUID;

public record RankingEntry(
    int position,
    UUID userId,
    String username,
    long wins,
    long totalGames,
    double winRate
) {}
