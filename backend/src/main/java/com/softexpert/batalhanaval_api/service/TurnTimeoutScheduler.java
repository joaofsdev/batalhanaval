package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.GameStatus;
import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private static final long PLACEMENT_TIMEOUT_SECONDS = 30;

    @Value("${game.afk.max-consecutive-skips:3}")
    private int maxConsecutiveSkips;

    private final GameRepository gameRepository;
    private final NotificationService notificationService;
    private final EloService eloService;

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void checkTurnTimeouts() {
        Instant cutoff = Instant.now().minusSeconds(TURN_TIMEOUT_SECONDS);

        List<Game> timedOutGames = gameRepository.findGamesWithExpiredTurn(GameStatus.IN_PROGRESS, cutoff);

        for (Game game : timedOutGames) {
            User currentPlayer = game.getCurrentTurn();
            if (currentPlayer == null) continue;

            game.setConsecutiveSkips(game.getConsecutiveSkips() + 1);

            if (game.getConsecutiveSkips() >= maxConsecutiveSkips) {
                // AFK defeat: current player loses
                User winner = game.getPlayer1().getId().equals(currentPlayer.getId())
                    ? game.getPlayer2()
                    : game.getPlayer1();

                game.setStatus(GameStatus.FINISHED);
                game.setWinner(winner);
                game.setCurrentTurn(null);
                eloService.updateElo(game);
                gameRepository.save(game);

                notificationService.broadcastGameState(game);

                log.info("AFK defeat: game={}, loser={} ({}  consecutive skips)",
                    game.getId(), currentPlayer.getUsername(), maxConsecutiveSkips);
            } else {
                // Skip turn
                User nextPlayer = game.getPlayer1().getId().equals(currentPlayer.getId())
                    ? game.getPlayer2()
                    : game.getPlayer1();

                game.setCurrentTurn(nextPlayer);
                gameRepository.save(game);

                notificationService.broadcastGameState(game);

                log.info("Turn timeout: game={}, skipped player={}, consecutive skips={}",
                    game.getId(), currentPlayer.getUsername(), game.getConsecutiveSkips());
            }
        }
    }

    /**
     * Cancels games stuck in PLACING status for more than 30 seconds when one player
     * has already placed their fleet but the opponent remains inactive.
     * This prevents players from being trapped waiting for an AFK opponent.
     */
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void checkPlacementTimeouts() {
        Instant cutoff = Instant.now().minusSeconds(PLACEMENT_TIMEOUT_SECONDS);

        List<Game> staleGames = gameRepository.findStalePlacingGames(cutoff);

        for (Game game : staleGames) {
            game.setStatus(GameStatus.FINISHED);
            game.setWinner(null);
            game.setCurrentTurn(null);
            gameRepository.save(game);

            notificationService.broadcastGameState(game);

            log.info("Placement timeout: game={} cancelled (opponent inactive for {}s)",
                game.getId(), PLACEMENT_TIMEOUT_SECONDS);
        }
    }
}
