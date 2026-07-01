package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.GameStatus;
import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TurnTimeoutScheduler {

    private static final long TURN_TIMEOUT_SECONDS = 60;

    private final GameRepository gameRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void checkTurnTimeouts() {
        Instant cutoff = Instant.now().minusSeconds(TURN_TIMEOUT_SECONDS);

        List<Game> timedOutGames = gameRepository.findGamesWithExpiredTurn(GameStatus.IN_PROGRESS, cutoff);

        for (Game game : timedOutGames) {
            User currentPlayer = game.getCurrentTurn();
            if (currentPlayer == null) continue;

            // Pass the turn to the other player (first timeout = skip turn)
            User nextPlayer = game.getPlayer1().getId().equals(currentPlayer.getId())
                ? game.getPlayer2()
                : game.getPlayer1();

            game.setCurrentTurn(nextPlayer);
            gameRepository.save(game);

            notificationService.broadcastGameState(game);

            log.info("Turn timeout: game={}, skipped player={}", game.getId(), currentPlayer.getUsername());
        }
    }
}
