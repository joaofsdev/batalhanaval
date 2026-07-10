package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.dto.response.RankingEntry;
import com.softexpert.batalhanaval_api.dto.response.RankingResponse;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final GameRepository gameRepository;

    @Transactional(readOnly = true)
    public RankingResponse getRanking(UUID currentUserId, String currentUsername, int page, int size, String period) {
        List<Object[]> rows = fetchRankingData(period);

        List<RankingEntry> allEntries = new ArrayList<>();
        RankingEntry myPosition = null;

        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            UUID userId = (UUID) row[0];
            String username = (String) row[1];
            long wins = (long) row[2];
            long totalGames = (long) row[3];
            int eloRating = (int) row[4];
            double winRate = totalGames > 0 ? Math.round((double) wins / totalGames * 1000.0) / 10.0 : 0.0;

            RankingEntry entry = new RankingEntry(i + 1, userId, username, wins, totalGames, winRate, eloRating);
            allEntries.add(entry);

            if (userId.equals(currentUserId)) {
                myPosition = entry;
            }
        }

        if (myPosition == null) {
            myPosition = new RankingEntry(allEntries.size() + 1, currentUserId, currentUsername, 0, 0, 0.0, 1000);
        }

        long totalElements = allEntries.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, allEntries.size());
        int toIndex = Math.min(fromIndex + size, allEntries.size());
        List<RankingEntry> pageContent = allEntries.subList(fromIndex, toIndex);

        return new RankingResponse(pageContent, myPosition, page, size, totalElements, totalPages);
    }

    private List<Object[]> fetchRankingData(String period) {
        if (period == null || period.isBlank() || "all".equalsIgnoreCase(period)) {
            return gameRepository.findFullRanking();
        }

        Instant since = switch (period.toLowerCase()) {
            case "week" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "month" -> Instant.now().minus(30, ChronoUnit.DAYS);
            default -> Instant.EPOCH;
        };

        return gameRepository.findFullRankingSince(since);
    }
}
