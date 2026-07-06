package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EloService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    /**
     * Update Elo ratings for both players after a game finishes.
     * Must be called after game.setWinner() has been set.
     * Persists eloBefore on the game and new eloRating on both users.
     */
    @Transactional
    public void updateElo(Game game) {
        User winner = game.getWinner();
        if (winner == null) {
            return; // No winner (e.g., draw or cancelled) — no Elo update
        }

        User player1 = game.getPlayer1();
        User player2 = game.getPlayer2();
        if (player2 == null) {
            return; // Single-player or incomplete game — no Elo update
        }

        int elo1 = player1.getEloRating();
        int elo2 = player2.getEloRating();

        // Persist elo before the update
        game.setPlayer1EloBefore(elo1);
        game.setPlayer2EloBefore(elo2);

        // Determine result from player1's perspective (1.0 = win, 0.0 = loss)
        double result1 = winner.getId().equals(player1.getId()) ? 1.0 : 0.0;
        double result2 = 1.0 - result1;

        // Calculate expected scores
        double expected1 = calculateExpectedScore(elo1, elo2);
        double expected2 = calculateExpectedScore(elo2, elo1);

        // Get K-factors based on number of finished games
        long games1 = gameRepository.countFinishedGamesByUserId(player1.getId());
        long games2 = gameRepository.countFinishedGamesByUserId(player2.getId());
        int k1 = determineKFactor(games1);
        int k2 = determineKFactor(games2);

        // Calculate new Elo ratings
        int newElo1 = calculateNewElo(elo1, k1, result1, expected1);
        int newElo2 = calculateNewElo(elo2, k2, result2, expected2);

        // Persist new ratings
        player1.setEloRating(newElo1);
        player2.setEloRating(newElo2);
        userRepository.save(player1);
        userRepository.save(player2);

        gameRepository.save(game);
    }

    /**
     * Calculate expected score for playerA against playerB.
     * Formula: 1 / (1 + 10^((eloB - eloA) / 400))
     */
    double calculateExpectedScore(int eloA, int eloB) {
        return 1.0 / (1.0 + Math.pow(10.0, (eloB - eloA) / 400.0));
    }

    /**
     * Determine K-factor based on number of finished games.
     * < 30 games  → K = 40 (provisional)
     * 30-100 games → K = 20 (intermediate)
     * > 100 games  → K = 10 (established)
     */
    int determineKFactor(long finishedGames) {
        if (finishedGames < 30) {
            return 40;
        } else if (finishedGames <= 100) {
            return 20;
        } else {
            return 10;
        }
    }

    /**
     * Calculate new Elo rating.
     * Formula: NewElo = OldElo + K * (Result - Expected)
     * Minimum Elo is 100 (floor to prevent negative/absurd ratings).
     */
    int calculateNewElo(int currentElo, int kFactor, double result, double expectedScore) {
        int newElo = (int) Math.round(currentElo + kFactor * (result - expectedScore));
        return Math.max(100, newElo);
    }
}
