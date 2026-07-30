package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.dto.response.RankingRow;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Separated bean for ranking data caching.
 * Returns List<RankingRow> instead of List<Object[]> for Redis JSON serialization.
 */
@Service
@RequiredArgsConstructor
public class RankingCacheService {

    private final GameRepository gameRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "ranking", key = "#period == null ? 'all' : #period")
    public List<RankingRow> fetchRankingData(String period) {
        List<Object[]> rows = fetchFromDb(period);
        return rows.stream()
                .map(row -> new RankingRow(
                        (UUID) row[0],
                        (String) row[1],
                        (long) row[2],
                        (long) row[3],
                        (int) row[4]
                ))
                .toList();
    }

    private List<Object[]> fetchFromDb(String period) {
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
