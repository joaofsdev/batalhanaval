package com.softexpert.batalhanaval_api.controller;

import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.dto.response.RankingEntry;
import com.softexpert.batalhanaval_api.dto.response.RankingResponse;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    @GetMapping
    public RankingResponse getRanking(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        UUID currentUserId = currentUser.getId();

        List<Object[]> rows = gameRepository.findWinsRanking();

        List<RankingEntry> top20 = new ArrayList<>();
        RankingEntry myPosition = null;

        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            UUID userId = (UUID) row[0];
            String username = (String) row[1];
            long wins = (long) row[2];
            long totalGames = gameRepository.countFinishedGamesByUserId(userId);

            RankingEntry entry = new RankingEntry(i + 1, userId, username, wins, totalGames);

            if (i < 20) {
                top20.add(entry);
            }

            if (userId.equals(currentUserId)) {
                myPosition = entry;
            }
        }

        // Se o jogador não está no ranking (nunca venceu), criar entry com 0 vitórias
        if (myPosition == null) {
            long totalGames = gameRepository.countFinishedGamesByUserId(currentUserId);
            myPosition = new RankingEntry(rows.size() + 1, currentUserId, currentUser.getUsername(), 0, totalGames);
        }

        return new RankingResponse(top20, myPosition);
    }
}
