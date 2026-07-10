package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.CancellationReason;
import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.GameStatus;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlacementTimeoutScheduler {

    private final GameRepository gameRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void checkPlacementTimeouts() {
        Instant now = Instant.now();

        List<Game> expiredGames = gameRepository.findPlacingGamesWithExpiredDeadline(GameStatus.PLACING, now);

        for (Game game : expiredGames) {
            if (game.getStatus() != GameStatus.PLACING) {
                continue;
            }

            try {
                game.setStatus(GameStatus.CANCELLED);
                game.setCancellationReason(CancellationReason.INACTIVITY);
                game.setCurrentTurn(null);
                gameRepository.save(game);

                notificationService.broadcastGameState(game);

                log.info("Placement timeout: game={} cancelled due to inactivity", game.getId());
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.info("Placement timeout skipped for game={} (concurrent status change)", game.getId());
            }
        }
    }
}
