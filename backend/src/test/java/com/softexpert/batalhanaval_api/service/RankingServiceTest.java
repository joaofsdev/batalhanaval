package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.dto.response.RankingEntry;
import com.softexpert.batalhanaval_api.dto.response.RankingResponse;
import com.softexpert.batalhanaval_api.dto.response.RankingRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock private RankingCacheService rankingCacheService;

    @InjectMocks private RankingService rankingService;

    private UUID user1Id;
    private UUID user2Id;
    private UUID user3Id;
    private UUID currentUserId;

    @BeforeEach
    void setUp() {
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();
        user3Id = UUID.randomUUID();
        currentUserId = UUID.randomUUID();
    }

    @Test
    void getRanking_shouldOrderByEloDescending() {
        List<RankingRow> rows = new ArrayList<>();
        rows.add(new RankingRow(user1Id, "highElo", 5L, 10L, 1200));
        rows.add(new RankingRow(user2Id, "midElo", 8L, 12L, 1100));
        rows.add(new RankingRow(user3Id, "lowElo", 10L, 15L, 900));

        when(rankingCacheService.fetchRankingData("all")).thenReturn(rows);

        RankingResponse response = rankingService.getRanking(currentUserId, "viewer", 0, 20, "all");

        assertThat(response.ranking()).hasSize(3);
        assertThat(response.ranking().get(0).username()).isEqualTo("highElo");
        assertThat(response.ranking().get(0).eloRating()).isEqualTo(1200);
        assertThat(response.ranking().get(0).position()).isEqualTo(1);

        assertThat(response.ranking().get(1).username()).isEqualTo("midElo");
        assertThat(response.ranking().get(1).eloRating()).isEqualTo(1100);
        assertThat(response.ranking().get(1).position()).isEqualTo(2);

        assertThat(response.ranking().get(2).username()).isEqualTo("lowElo");
        assertThat(response.ranking().get(2).eloRating()).isEqualTo(900);
        assertThat(response.ranking().get(2).position()).isEqualTo(3);
    }

    @Test
    void getRanking_sameElo_shouldTiebreakByWinsDescending() {
        List<RankingRow> rows = new ArrayList<>();
        rows.add(new RankingRow(user1Id, "moreWins", 8L, 10L, 1000));
        rows.add(new RankingRow(user2Id, "fewerWins", 3L, 10L, 1000));

        when(rankingCacheService.fetchRankingData("all")).thenReturn(rows);

        RankingResponse response = rankingService.getRanking(currentUserId, "viewer", 0, 20, "all");

        assertThat(response.ranking()).hasSize(2);
        assertThat(response.ranking().get(0).username()).isEqualTo("moreWins");
        assertThat(response.ranking().get(0).wins()).isEqualTo(8);
        assertThat(response.ranking().get(0).position()).isEqualTo(1);

        assertThat(response.ranking().get(1).username()).isEqualTo("fewerWins");
        assertThat(response.ranking().get(1).wins()).isEqualTo(3);
        assertThat(response.ranking().get(1).position()).isEqualTo(2);
    }

    @Test
    void getRanking_emptyRanking_playerNotInRanking_shouldGetDefaultPosition() {
        when(rankingCacheService.fetchRankingData("all")).thenReturn(new ArrayList<>());

        RankingResponse response = rankingService.getRanking(currentUserId, "newPlayer", 0, 20, "all");

        assertThat(response.ranking()).isEmpty();
        assertThat(response.myPosition()).isNotNull();
        assertThat(response.myPosition().position()).isEqualTo(1);
        assertThat(response.myPosition().username()).isEqualTo("newPlayer");
        assertThat(response.myPosition().eloRating()).isEqualTo(1000);
        assertThat(response.myPosition().wins()).isEqualTo(0);
    }

    @Test
    void getRanking_currentUserInRanking_shouldFindTheirPosition() {
        List<RankingRow> rows = new ArrayList<>();
        rows.add(new RankingRow(user1Id, "topPlayer", 10L, 12L, 1300));
        rows.add(new RankingRow(currentUserId, "me", 5L, 10L, 1100));
        rows.add(new RankingRow(user2Id, "other", 3L, 8L, 900));

        when(rankingCacheService.fetchRankingData("all")).thenReturn(rows);

        RankingResponse response = rankingService.getRanking(currentUserId, "me", 0, 20, "all");

        assertThat(response.myPosition()).isNotNull();
        assertThat(response.myPosition().position()).isEqualTo(2);
        assertThat(response.myPosition().eloRating()).isEqualTo(1100);
        assertThat(response.myPosition().username()).isEqualTo("me");
    }

    @Test
    void getRanking_shouldIncludeEloRatingInEntries() {
        List<RankingRow> rows = new ArrayList<>();
        rows.add(new RankingRow(user1Id, "player1", 7L, 10L, 1150));

        when(rankingCacheService.fetchRankingData("all")).thenReturn(rows);

        RankingResponse response = rankingService.getRanking(currentUserId, "viewer", 0, 20, "all");

        RankingEntry entry = response.ranking().get(0);
        assertThat(entry.eloRating()).isEqualTo(1150);
        assertThat(entry.wins()).isEqualTo(7);
        assertThat(entry.totalGames()).isEqualTo(10);
        assertThat(entry.winRate()).isEqualTo(70.0);
    }

    @Test
    void getRanking_withPeriodWeek_shouldCallFetchRankingDataWithWeek() {
        when(rankingCacheService.fetchRankingData("week")).thenReturn(new ArrayList<>());

        rankingService.getRanking(currentUserId, "viewer", 0, 20, "week");

        verify(rankingCacheService).fetchRankingData("week");
    }

    @Test
    void getRanking_pagination_shouldReturnCorrectPage() {
        List<RankingRow> rows = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            rows.add(new RankingRow(UUID.randomUUID(), "player" + i, (long)(25 - i), 30L, 1500 - i * 10));
        }

        when(rankingCacheService.fetchRankingData("all")).thenReturn(rows);

        // Page 0, size 10
        RankingResponse page0 = rankingService.getRanking(currentUserId, "viewer", 0, 10, "all");
        assertThat(page0.ranking()).hasSize(10);
        assertThat(page0.ranking().get(0).position()).isEqualTo(1);
        assertThat(page0.totalElements()).isEqualTo(25);
        assertThat(page0.totalPages()).isEqualTo(3);

        // Page 2, size 10 (last page with 5 items)
        RankingResponse page2 = rankingService.getRanking(currentUserId, "viewer", 2, 10, "all");
        assertThat(page2.ranking()).hasSize(5);
        assertThat(page2.ranking().get(0).position()).isEqualTo(21);
    }
}
