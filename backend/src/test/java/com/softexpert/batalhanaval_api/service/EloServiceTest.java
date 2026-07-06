package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.Game;
import com.softexpert.batalhanaval_api.domain.GameStatus;
import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.repository.GameRepository;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EloServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private GameRepository gameRepository;

    @InjectMocks private EloService eloService;

    private User player1;
    private User player2;
    private Game game;

    @BeforeEach
    void setUp() {
        player1 = new User();
        player1.setId(UUID.randomUUID());
        player1.setUsername("player1");
        player1.setEloRating(1000);

        player2 = new User();
        player2.setId(UUID.randomUUID());
        player2.setUsername("player2");
        player2.setEloRating(1000);

        game = new Game();
        game.setId(UUID.randomUUID());
        game.setPlayer1(player1);
        game.setPlayer2(player2);
        game.setStatus(GameStatus.FINISHED);
    }

    // --- Expected Score Tests ---

    @Test
    void calculateExpectedScore_equalElo_shouldReturn0_5() {
        double expected = eloService.calculateExpectedScore(1000, 1000);
        assertThat(expected).isCloseTo(0.5, within(0.0001));
    }

    @Test
    void calculateExpectedScore_higherElo_shouldReturnAbove0_5() {
        double expected = eloService.calculateExpectedScore(1200, 1000);
        assertThat(expected).isGreaterThan(0.5);
        // Expected ≈ 0.7597
        assertThat(expected).isCloseTo(0.7597, within(0.001));
    }

    @Test
    void calculateExpectedScore_lowerElo_shouldReturnBelow0_5() {
        double expected = eloService.calculateExpectedScore(1000, 1200);
        assertThat(expected).isLessThan(0.5);
        // Expected ≈ 0.2403
        assertThat(expected).isCloseTo(0.2403, within(0.001));
    }

    @Test
    void calculateExpectedScore_largeDifference_shouldApproachExtreme() {
        // 500+ point difference
        double expected = eloService.calculateExpectedScore(1500, 1000);
        assertThat(expected).isGreaterThan(0.9);
    }

    @Test
    void calculateExpectedScore_symmetry_shouldSumTo1() {
        double expectedA = eloService.calculateExpectedScore(1200, 1000);
        double expectedB = eloService.calculateExpectedScore(1000, 1200);
        assertThat(expectedA + expectedB).isCloseTo(1.0, within(0.0001));
    }

    // --- K-Factor Tests ---

    @Test
    void determineKFactor_newPlayer_shouldReturn40() {
        assertThat(eloService.determineKFactor(0)).isEqualTo(40);
        assertThat(eloService.determineKFactor(10)).isEqualTo(40);
        assertThat(eloService.determineKFactor(29)).isEqualTo(40);
    }

    @Test
    void determineKFactor_intermediatePlayer_shouldReturn20() {
        assertThat(eloService.determineKFactor(30)).isEqualTo(20);
        assertThat(eloService.determineKFactor(50)).isEqualTo(20);
        assertThat(eloService.determineKFactor(100)).isEqualTo(20);
    }

    @Test
    void determineKFactor_veteranPlayer_shouldReturn10() {
        assertThat(eloService.determineKFactor(101)).isEqualTo(10);
        assertThat(eloService.determineKFactor(500)).isEqualTo(10);
    }

    // --- New Elo Calculation Tests ---

    @Test
    void calculateNewElo_winAgainstEqualOpponent_shouldIncrease() {
        // K=40, result=1.0, expected=0.5 → delta = 40*(1.0-0.5) = +20
        int newElo = eloService.calculateNewElo(1000, 40, 1.0, 0.5);
        assertThat(newElo).isEqualTo(1020);
    }

    @Test
    void calculateNewElo_lossAgainstEqualOpponent_shouldDecrease() {
        // K=40, result=0.0, expected=0.5 → delta = 40*(0.0-0.5) = -20
        int newElo = eloService.calculateNewElo(1000, 40, 0.0, 0.5);
        assertThat(newElo).isEqualTo(980);
    }

    @Test
    void calculateNewElo_winAgainstStrongerOpponent_shouldIncreaseMore() {
        // 1000 vs 1200: expected ≈ 0.2403, K=40, win → delta = 40*(1.0-0.2403) ≈ +30
        double expected = eloService.calculateExpectedScore(1000, 1200);
        int newElo = eloService.calculateNewElo(1000, 40, 1.0, expected);
        assertThat(newElo).isGreaterThan(1020); // More than winning against equal
        assertThat(newElo).isEqualTo(1030); // 40*(1.0-0.2403) ≈ 30.39 → round to 30
    }

    @Test
    void calculateNewElo_lossAgainstWeakerOpponent_shouldDecreaseMore() {
        // 1200 vs 1000: expected ≈ 0.7597, K=20, loss → delta = 20*(0.0-0.7597) ≈ -15
        double expected = eloService.calculateExpectedScore(1200, 1000);
        int newElo = eloService.calculateNewElo(1200, 20, 0.0, expected);
        assertThat(newElo).isLessThan(1200);
        assertThat(newElo).isEqualTo(1185); // 1200 + 20*(0.0-0.7597) ≈ 1200-15.19 → 1185
    }

    @Test
    void calculateNewElo_shouldNotDropBelow100() {
        // Very low Elo with a loss
        int newElo = eloService.calculateNewElo(100, 40, 0.0, 0.5);
        assertThat(newElo).isEqualTo(100); // Floor at 100 (100 + 40*(0-0.5) = 80, clamped to 100)
    }

    // --- Full updateElo Integration Tests ---

    @Test
    void updateElo_player1Wins_equalElo_bothNewPlayers() {
        game.setWinner(player1);
        when(gameRepository.countFinishedGamesByUserId(player1.getId())).thenReturn(5L);
        when(gameRepository.countFinishedGamesByUserId(player2.getId())).thenReturn(5L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        eloService.updateElo(game);

        // Both are new players (K=40), equal Elo (expected=0.5)
        // Winner: 1000 + 40*(1.0-0.5) = 1020
        // Loser:  1000 + 40*(0.0-0.5) = 980
        assertThat(player1.getEloRating()).isEqualTo(1020);
        assertThat(player2.getEloRating()).isEqualTo(980);
        assertThat(game.getPlayer1EloBefore()).isEqualTo(1000);
        assertThat(game.getPlayer2EloBefore()).isEqualTo(1000);
    }

    @Test
    void updateElo_player2Wins_equalElo_bothNewPlayers() {
        game.setWinner(player2);
        when(gameRepository.countFinishedGamesByUserId(player1.getId())).thenReturn(10L);
        when(gameRepository.countFinishedGamesByUserId(player2.getId())).thenReturn(10L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        eloService.updateElo(game);

        // Player2 wins: result1=0.0, result2=1.0
        assertThat(player1.getEloRating()).isEqualTo(980);
        assertThat(player2.getEloRating()).isEqualTo(1020);
    }

    @Test
    void updateElo_underdogWins_shouldGainMore() {
        player1.setEloRating(1000); // underdog
        player2.setEloRating(1200); // favorite
        game.setWinner(player1);
        when(gameRepository.countFinishedGamesByUserId(player1.getId())).thenReturn(5L);  // K=40
        when(gameRepository.countFinishedGamesByUserId(player2.getId())).thenReturn(50L); // K=20
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        eloService.updateElo(game);

        // Player1 (1000) beats Player2 (1200)
        // Expected1 ≈ 0.2403, K1=40 → 1000 + 40*(1.0-0.2403) = 1000 + 30.39 ≈ 1030
        // Expected2 ≈ 0.7597, K2=20 → 1200 + 20*(0.0-0.7597) = 1200 - 15.19 ≈ 1185
        assertThat(player1.getEloRating()).isEqualTo(1030);
        assertThat(player2.getEloRating()).isEqualTo(1185);
        assertThat(game.getPlayer1EloBefore()).isEqualTo(1000);
        assertThat(game.getPlayer2EloBefore()).isEqualTo(1200);
    }

    @Test
    void updateElo_favoriteWins_shouldGainLess() {
        player1.setEloRating(1200); // favorite
        player2.setEloRating(1000); // underdog
        game.setWinner(player1);
        when(gameRepository.countFinishedGamesByUserId(player1.getId())).thenReturn(50L);  // K=20
        when(gameRepository.countFinishedGamesByUserId(player2.getId())).thenReturn(5L);   // K=40
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        eloService.updateElo(game);

        // Player1 (1200) beats Player2 (1000)
        // Expected1 ≈ 0.7597, K1=20 → 1200 + 20*(1.0-0.7597) = 1200 + 4.81 ≈ 1205
        // Expected2 ≈ 0.2403, K2=40 → 1000 + 40*(0.0-0.2403) = 1000 - 9.61 ≈ 990
        assertThat(player1.getEloRating()).isEqualTo(1205);
        assertThat(player2.getEloRating()).isEqualTo(990);
    }

    @Test
    void updateElo_veteranVsVeteran_shouldChangeSmaller() {
        player1.setEloRating(1100);
        player2.setEloRating(1100);
        game.setWinner(player1);
        when(gameRepository.countFinishedGamesByUserId(player1.getId())).thenReturn(150L); // K=10
        when(gameRepository.countFinishedGamesByUserId(player2.getId())).thenReturn(200L); // K=10
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        eloService.updateElo(game);

        // Both K=10, equal Elo, expected=0.5
        // Winner: 1100 + 10*(1.0-0.5) = 1105
        // Loser:  1100 + 10*(0.0-0.5) = 1095
        assertThat(player1.getEloRating()).isEqualTo(1105);
        assertThat(player2.getEloRating()).isEqualTo(1095);
    }

    @Test
    void updateElo_noWinner_shouldDoNothing() {
        game.setWinner(null);

        eloService.updateElo(game);

        assertThat(player1.getEloRating()).isEqualTo(1000);
        assertThat(player2.getEloRating()).isEqualTo(1000);
        assertThat(game.getPlayer1EloBefore()).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateElo_noPlayer2_shouldDoNothing() {
        game.setPlayer2(null);
        game.setWinner(player1);

        eloService.updateElo(game);

        assertThat(player1.getEloRating()).isEqualTo(1000);
        assertThat(game.getPlayer1EloBefore()).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateElo_shouldPersistEloBefore() {
        player1.setEloRating(1150);
        player2.setEloRating(950);
        game.setWinner(player2);
        when(gameRepository.countFinishedGamesByUserId(any())).thenReturn(20L); // K=40
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        eloService.updateElo(game);

        assertThat(game.getPlayer1EloBefore()).isEqualTo(1150);
        assertThat(game.getPlayer2EloBefore()).isEqualTo(950);
    }

    @Test
    void updateElo_shouldSaveBothUsersAndGame() {
        game.setWinner(player1);
        when(gameRepository.countFinishedGamesByUserId(any())).thenReturn(5L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        eloService.updateElo(game);

        verify(userRepository, times(2)).save(any(User.class));
        verify(gameRepository).save(game);
    }
}
