package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.response.GameResponse;
import com.softexpert.batalhanaval_api.exception.*;
import com.softexpert.batalhanaval_api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock private GameRepository gameRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private UserRepository userRepository;
    @Mock private ShotRepository shotRepository;
    @Mock private PlacementService placementService;
    @Mock private AbilityService abilityService;
    @Mock private EloService eloService;

    @InjectMocks private GameService gameService;

    private User player1;
    private User player2;

    @BeforeEach
    void setUp() {
        player1 = new User();
        player1.setId(UUID.randomUUID());
        player1.setUsername("player1");

        player2 = new User();
        player2.setId(UUID.randomUUID());
        player2.setUsername("player2");
    }

    @Test
    void createGame_noWaiting_shouldCreateNew() {
        when(gameRepository.findActiveGameByUserId(eq(player1.getId()), any())).thenReturn(Optional.empty());
        when(userRepository.findById(player1.getId())).thenReturn(Optional.of(player1));
        when(gameRepository.findByStatusAndPlayer1IdNot(GameStatus.WAITING, player1.getId())).thenReturn(List.of());
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> {
            Game g = inv.getArgument(0);
            g.setId(UUID.randomUUID());
            g.setCreatedAt(Instant.now());
            return g;
        });
        when(boardRepository.save(any(Board.class))).thenAnswer(inv -> inv.getArgument(0));

        GameResponse response = gameService.createOrJoinGame(player1.getId());

        assertThat(response.status()).isEqualTo(GameStatus.WAITING);
        assertThat(response.player1().username()).isEqualTo("player1");
        assertThat(response.player2()).isNull();
    }

    @Test
    void joinGame_existingWaiting_shouldJoin() {
        Game waitingGame = new Game();
        waitingGame.setId(UUID.randomUUID());
        waitingGame.setPlayer1(player1);
        waitingGame.setStatus(GameStatus.WAITING);
        waitingGame.setBoards(new ArrayList<>());
        waitingGame.setCreatedAt(Instant.now());

        when(gameRepository.findActiveGameByUserId(eq(player2.getId()), any())).thenReturn(Optional.empty());
        when(userRepository.findById(player2.getId())).thenReturn(Optional.of(player2));
        when(gameRepository.findByStatusAndPlayer1IdNot(GameStatus.WAITING, player2.getId())).thenReturn(List.of(waitingGame));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        when(boardRepository.save(any(Board.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shotRepository.findAllByGameIdAndAttackerId(any(), any())).thenReturn(List.of());

        GameResponse response = gameService.createOrJoinGame(player2.getId());

        assertThat(response.status()).isEqualTo(GameStatus.PLACING);
        assertThat(response.player2().username()).isEqualTo("player2");
    }

    @Test
    void createGame_playerAlreadyInGame_shouldThrow() {
        Game activeGame = new Game();
        activeGame.setStatus(GameStatus.IN_PROGRESS);

        when(gameRepository.findActiveGameByUserId(eq(player1.getId()), any())).thenReturn(Optional.of(activeGame));

        assertThatThrownBy(() -> gameService.createOrJoinGame(player1.getId()))
            .isInstanceOf(PlayerAlreadyInGameException.class);
    }

    @Test
    void getGameState_notParticipant_shouldThrow() {
        Game game = new Game();
        game.setId(UUID.randomUUID());
        game.setPlayer1(player1);
        game.setPlayer2(player2);
        game.setStatus(GameStatus.IN_PROGRESS);

        UUID outsider = UUID.randomUUID();
        when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.getGameState(game.getId(), outsider))
            .isInstanceOf(NotGameParticipantException.class);
    }

    @Test
    void getGameState_gameNotFound_shouldThrow() {
        UUID fakeId = UUID.randomUUID();
        when(gameRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.getGameState(fakeId, player1.getId()))
            .isInstanceOf(GameNotFoundException.class);
    }
}
