package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.dto.response.GameHistoryEntry;
import com.softexpert.batalhanaval_api.dto.response.PlayerProfileResponse;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.softexpert.batalhanaval_api.exception.UserNotFoundException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final RankingService rankingService;

    @Transactional(readOnly = true)
    public PlayerProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        // Stats
        List<Object[]> rankingRows = gameRepository.findFullRanking();
        long totalGames = 0;
        long wins = 0;
        int rank = rankingRows.size() + 1;

        for (int i = 0; i < rankingRows.size(); i++) {
            Object[] row = rankingRows.get(i);
            UUID id = (UUID) row[0];
            if (id.equals(userId)) {
                wins = (long) row[2];
                totalGames = (long) row[3];
                rank = i + 1;
                break;
            }
        }

        // If not found in ranking (no finished games), count anyway
        if (totalGames == 0) {
            totalGames = gameRepository.countFinishedGamesByUserId(userId);
        }

        long losses = totalGames - wins;
        double winRate = totalGames > 0 ? Math.round((double) wins / totalGames * 1000.0) / 10.0 : 0.0;

        // Recent games (last 5)
        List<Game> recentGames = gameRepository.findFinishedGamesByUserId(userId, PageRequest.of(0, 5)).getContent();
        List<GameHistoryEntry> recentEntries = recentGames.stream().map(game -> {
            String opponentUsername;
            if (game.getPlayer1().getId().equals(userId)) {
                opponentUsername = game.getPlayer2() != null ? game.getPlayer2().getUsername() : "Desconhecido";
            } else {
                opponentUsername = game.getPlayer1().getUsername();
            }
            boolean won = game.getWinner() != null && game.getWinner().getId().equals(userId);
            long durationSeconds = Duration.between(game.getCreatedAt(), game.getUpdatedAt()).getSeconds();
            return new GameHistoryEntry(game.getId(), opponentUsername, game.getStatus(), won, durationSeconds, game.getUpdatedAt());
        }).toList();

        return new PlayerProfileResponse(
            user.getId(),
            user.getUsername(),
            totalGames,
            wins,
            losses,
            winRate,
            rank,
            user.getCreatedAt(),
            recentEntries
        );
    }
}
