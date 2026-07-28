package com.softexpert.batalhanaval_api.config.observability;

import com.softexpert.batalhanaval_api.domain.GameStatus;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import com.softexpert.batalhanaval_api.websocket.WebSocketEventListener;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Configuration;

/**
 * Registers game-specific business metrics as Micrometer gauges.
 * Uses SmartInitializingSingleton to ensure MeterRegistry is already available.
 */
@Configuration
public class GameMetrics implements SmartInitializingSingleton {

    private final MeterRegistry meterRegistry;
    private final GameRepository gameRepository;
    private final WebSocketEventListener webSocketEventListener;
    private final OnlinePlayersTracker onlinePlayersTracker;

    public GameMetrics(MeterRegistry meterRegistry,
                       GameRepository gameRepository,
                       WebSocketEventListener webSocketEventListener,
                       OnlinePlayersTracker onlinePlayersTracker) {
        this.meterRegistry = meterRegistry;
        this.gameRepository = gameRepository;
        this.webSocketEventListener = webSocketEventListener;
        this.onlinePlayersTracker = onlinePlayersTracker;
    }

    @Override
    public void afterSingletonsInstantiated() {
        // Players online (authenticated request in the last 5 minutes)
        Gauge.builder("game.players.online", onlinePlayersTracker::getOnlineCount)
                .description("Number of players active in the last 5 minutes")
                .register(meterRegistry);

        // WebSocket connections (total open sessions)
        Gauge.builder("game.websocket.connections", webSocketEventListener::getTotalSessionCount)
                .description("Total number of open WebSocket connections")
                .register(meterRegistry);

        // Active games (IN_PROGRESS)
        Gauge.builder("game.matches.active", () -> gameRepository.countByStatus(GameStatus.IN_PROGRESS))
                .description("Number of games currently in progress")
                .register(meterRegistry);

        // Waiting games (WAITING for opponent)
        Gauge.builder("game.matches.waiting", () -> gameRepository.countByStatus(GameStatus.WAITING))
                .description("Number of games waiting for an opponent")
                .register(meterRegistry);

        // Placing games (PLACING fleet)
        Gauge.builder("game.matches.placing", () -> gameRepository.countByStatus(GameStatus.PLACING))
                .description("Number of games in fleet placement phase")
                .register(meterRegistry);
    }
}
