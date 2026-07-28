package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Separated bean for ranking data caching.
 * Spring AOP proxy requires @Cacheable to be on a different bean than the caller.
 */
@Service
@RequiredArgsConstructor
public class RankingCacheService {

    private final GameRepository gameRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "ranking", key = "#period == null ? 'all' : #period")
    public List<Object[]> fetchRankingData(String period) {
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
