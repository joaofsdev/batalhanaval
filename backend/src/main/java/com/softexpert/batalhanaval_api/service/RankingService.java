package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.dto.response.RankingEntry;
import com.softexpert.batalhanaval_api.dto.response.RankingResponse;
import com.softexpert.batalhanaval_api.dto.response.RankingRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final RankingCacheService rankingCacheService;

    public RankingResponse getRanking(UUID currentUserId, String currentUsername, int page, int size, String period) {
        List<RankingRow> rows = rankingCacheService.fetchRankingData(period);

        List<RankingEntry> allEntries = new ArrayList<>();
        RankingEntry myPosition = null;

        for (int i = 0; i < rows.size(); i++) {
            RankingRow row = rows.get(i);
            double winRate = row.totalGames() > 0 ? Math.round((double) row.wins() / row.totalGames() * 1000.0) / 10.0 : 0.0;

            RankingEntry entry = new RankingEntry(i + 1, row.userId(), row.username(), row.wins(), row.totalGames(), winRate, row.eloRating());
            allEntries.add(entry);

            if (row.userId().equals(currentUserId)) {
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
}
