package com.softexpert.batalhanaval_api.dto.response;

import java.io.Serializable;
import java.util.UUID;

/**
 * Lightweight DTO for caching ranking data in Redis.
 * Replaces Object[] from JPA query to enable proper JSON serialization.
 */
public record RankingRow(
    UUID userId,
    String username,
    long wins,
    long totalGames,
    int eloRating
) implements Serializable {}
