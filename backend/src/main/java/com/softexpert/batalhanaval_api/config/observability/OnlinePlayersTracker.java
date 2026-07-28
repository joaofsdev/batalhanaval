package com.softexpert.batalhanaval_api.config.observability;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tracks player online status based on their last authenticated request.
 * A player is considered "online" if they made any request in the last 5 minutes.
 * Cleanup runs every 5 minutes to evict inactive players.
 */
@Component
public class OnlinePlayersTracker {

    private static final long ONLINE_THRESHOLD_SECONDS = 300; // 5 minutes

    private final Map<UUID, Instant> lastActivity = new ConcurrentHashMap<>();

    /**
     * Records activity for a user (called on every authenticated request).
     */
    public void recordActivity(UUID userId) {
        lastActivity.put(userId, Instant.now());
    }

    /**
     * Returns the number of players considered online (active in the last 5 minutes).
     */
    public int getOnlineCount() {
        return lastActivity.size();
    }

    /**
     * Evicts players who haven't made a request in the last 5 minutes.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000)
    public void evictInactivePlayers() {
        Instant threshold = Instant.now().minusSeconds(ONLINE_THRESHOLD_SECONDS);
        lastActivity.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
    }
}
