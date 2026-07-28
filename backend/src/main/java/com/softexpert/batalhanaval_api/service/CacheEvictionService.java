package com.softexpert.batalhanaval_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * Handles cache eviction when game state changes (match ends, surrender, AFK).
 */
@Service
@RequiredArgsConstructor
public class CacheEvictionService {

    private final CacheManager cacheManager;

    /**
     * Evicts ranking cache (all entries) and profile cache for both players.
     * Called when a game finishes, is surrendered, or cancelled by AFK.
     */
    public void evictOnGameEnd(UUID player1Id, UUID player2Id) {
        // Evict all ranking entries (different periods/pages)
        Objects.requireNonNull(cacheManager.getCache("ranking")).clear();

        // Evict profiles of both players
        Objects.requireNonNull(cacheManager.getCache("profile")).evict(player1Id);
        if (player2Id != null) {
            Objects.requireNonNull(cacheManager.getCache("profile")).evict(player2Id);
        }
    }
}
