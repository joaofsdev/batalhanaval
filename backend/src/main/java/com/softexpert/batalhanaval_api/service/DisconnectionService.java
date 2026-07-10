package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.CancellationReason;
import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.GameStatus;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisconnectionService {

    private static final long IN_PROGRESS_GRACE_PERIOD_SECONDS = 30;
    private static final long PLACING_GRACE_PERIOD_SECONDS = 60;

    private final GameRepository gameRepository;
    private final GameService gameService;
    private final NotificationService notificationService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<UUID, ScheduledFuture<?>> pendingTimeouts = new ConcurrentHashMap<>();

    public void handleDisconnect(UUID userId) {
        gameService.getPendingRematches().entrySet().removeIf(entry -> entry.getValue().equals(userId));

        gameRepository.findActiveGameByUserId(userId, List.of(GameStatus.IN_PROGRESS))
            .ifPresent(game -> {
                log.info("Player {} disconnected from IN_PROGRESS game {}. Starting {}s grace period.",
                    userId, game.getId(), IN_PROGRESS_GRACE_PERIOD_SECONDS);

                notificationService.notifyOpponentDisconnected(game.getId(), (int) IN_PROGRESS_GRACE_PERIOD_SECONDS);

                ScheduledFuture<?> future = scheduler.schedule(
                    () -> applyDisconnectionLoss(game.getId(), userId),
                    IN_PROGRESS_GRACE_PERIOD_SECONDS,
                    TimeUnit.SECONDS
                );

                pendingTimeouts.put(userId, future);
            });

        gameRepository.findActiveGameByUserId(userId, List.of(GameStatus.PLACING))
            .ifPresent(game -> {
                log.info("Player {} disconnected from PLACING game {}. Starting {}s grace period.",
                    userId, game.getId(), PLACING_GRACE_PERIOD_SECONDS);

                notificationService.notifyOpponentDisconnected(game.getId(), (int) PLACING_GRACE_PERIOD_SECONDS);

                ScheduledFuture<?> future = scheduler.schedule(
                    () -> cancelPlacementGame(game.getId(), userId),
                    PLACING_GRACE_PERIOD_SECONDS,
                    TimeUnit.SECONDS
                );

                pendingTimeouts.put(userId, future);
            });
    }

    public void handleReconnect(UUID userId) {
        ScheduledFuture<?> future = pendingTimeouts.remove(userId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            log.info("Player {} reconnected. Grace period cancelled.", userId);

            gameRepository.findActiveGameByUserId(userId, List.of(GameStatus.IN_PROGRESS))
                .ifPresent(game -> notificationService.notifyOpponentReconnected(game.getId()));

            gameRepository.findActiveGameByUserId(userId, List.of(GameStatus.PLACING))
                .ifPresent(game -> notificationService.notifyOpponentReconnected(game.getId()));
        }
    }

    private void applyDisconnectionLoss(UUID gameId, UUID userId) {
        try {
            pendingTimeouts.remove(userId);

            Game game = gameRepository.findById(gameId).orElse(null);
            if (game == null || game.getStatus() != GameStatus.IN_PROGRESS) {
                return;
            }

            log.info("Grace period expired for player {} in game {}. Applying automatic loss.",
                userId, gameId);

            Game updatedGame = gameService.surrender(gameId, userId);
            notificationService.broadcastGameState(updatedGame);
        } catch (Exception ex) {
            log.error("Error applying disconnection loss for player {} in game {}", userId, gameId, ex);
        }
    }

    private void cancelPlacementGame(UUID gameId, UUID userId) {
        try {
            pendingTimeouts.remove(userId);

            Game game = gameRepository.findById(gameId).orElse(null);
            if (game == null || game.getStatus() != GameStatus.PLACING) {
                return;
            }

            game.setStatus(GameStatus.CANCELLED);
            game.setCancellationReason(CancellationReason.DISCONNECTION);
            game.setCurrentTurn(null);
            gameRepository.save(game);

            notificationService.broadcastGameState(game);

            log.info("Placement disconnection timeout: game={} cancelled, disconnected player={}",
                gameId, userId);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.info("Placement cancellation skipped for game={} (concurrent status change)", gameId);
        } catch (Exception ex) {
            log.error("Error cancelling placement game {} for disconnected player {}", gameId, userId, ex);
        }
    }
}
