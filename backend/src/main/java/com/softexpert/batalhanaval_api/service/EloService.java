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

    @Transactional
    public void updateElo(Game game) {
        if (!game.isRanked()) {
            return;
        }

        User winner = game.getWinner();
        if (winner == null) {
            return;
        }

        User player1 = game.getPlayer1();
        User player2 = game.getPlayer2();
        if (player2 == null) {
            return;
        }

        int elo1 = player1.getEloRating();
        int elo2 = player2.getEloRating();

        game.setPlayer1EloBefore(elo1);
        game.setPlayer2EloBefore(elo2);

        double result1 = winner.getId().equals(player1.getId()) ? 1.0 : 0.0;
        double result2 = 1.0 - result1;

        double expected1 = calculateExpectedScore(elo1, elo2);
        double expected2 = calculateExpectedScore(elo2, elo1);

        long games1 = gameRepository.countFinishedGamesByUserId(player1.getId());
        long games2 = gameRepository.countFinishedGamesByUserId(player2.getId());
        int k1 = determineKFactor(games1);
        int k2 = determineKFactor(games2);

        int newElo1 = calculateNewElo(elo1, k1, result1, expected1);
        int newElo2 = calculateNewElo(elo2, k2, result2, expected2);

        player1.setEloRating(newElo1);
        player2.setEloRating(newElo2);
        userRepository.save(player1);
        userRepository.save(player2);

        gameRepository.save(game);
    }

    double calculateExpectedScore(int eloA, int eloB) {
        return 1.0 / (1.0 + Math.pow(10.0, (eloB - eloA) / 400.0));
    }

    int determineKFactor(long finishedGames) {
        if (finishedGames < 30) {
            return 40;
        } else if (finishedGames <= 100) {
            return 20;
        } else {
            return 10;
        }
    }

    int calculateNewElo(int currentElo, int kFactor, double result, double expectedScore) {
        int newElo = (int) Math.round(currentElo + kFactor * (result - expectedScore));
        return Math.max(100, newElo);
    }
}
