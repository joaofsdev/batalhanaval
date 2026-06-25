package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.response.ShotResultResponse;
import com.softexpert.batalhanaval_api.exception.*;
import com.softexpert.batalhanaval_api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShotServiceTest {

    @Mock private GameRepository gameRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private CellRepository cellRepository;
    @Mock private ShipRepository shipRepository;
    @Mock private ShotRepository shotRepository;

    @InjectMocks private ShotService shotService;

    private Game game;
    private User attacker;
    private User defender;
    private Board targetBoard;
    private UUID gameId;

    @BeforeEach
    void setUp() {
        gameId = UUID.randomUUID();
        attacker = new User();
        attacker.setId(UUID.randomUUID());
        defender = new User();
        defender.setId(UUID.randomUUID());

        game = new Game();
        game.setId(gameId);
        game.setPlayer1(attacker);
        game.setPlayer2(defender);
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setCurrentTurn(attacker);

        targetBoard = new Board();
        targetBoard.setId(UUID.randomUUID());
        targetBoard.setOwner(defender);
    }

    @Test
    void miss_shouldReturnMissAndAlternateTurn() {
        Cell cell = new Cell();
        cell.setHasShip(false);
        cell.setHit(false);

        Ship aliveShip = new Ship();
        aliveShip.setShipType(ShipType.CARRIER);
        aliveShip.setHits(0);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(boardRepository.findByGameIdAndOwnerId(gameId, defender.getId())).thenReturn(Optional.of(targetBoard));
        when(cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), 3, 7)).thenReturn(Optional.of(cell));
        when(shipRepository.findAllByBoardId(targetBoard.getId())).thenReturn(List.of(aliveShip));

        ShotResultResponse result = shotService.processShot(gameId, attacker.getId(), 3, 7);

        assertThat(result.result()).isEqualTo(ShotResult.MISS);
        assertThat(cell.isHit()).isTrue();
        assertThat(game.getCurrentTurn()).isEqualTo(defender);
    }

    @Test
    void hit_shouldReturnHitAndIncrementShipHits() {
        Ship ship = new Ship();
        ship.setShipType(ShipType.CRUISER);
        ship.setHits(0);

        Cell cell = new Cell();
        cell.setHasShip(true);
        cell.setHit(false);
        cell.setShip(ship);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(boardRepository.findByGameIdAndOwnerId(gameId, defender.getId())).thenReturn(Optional.of(targetBoard));
        when(cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), 2, 3)).thenReturn(Optional.of(cell));
        when(shipRepository.findAllByBoardId(targetBoard.getId())).thenReturn(List.of(ship));

        ShotResultResponse result = shotService.processShot(gameId, attacker.getId(), 2, 3);

        assertThat(result.result()).isEqualTo(ShotResult.HIT);
        assertThat(ship.getHits()).isEqualTo(1);
    }

    @Test
    void sunk_shouldReturnSunkWithShipType() {
        Ship ship = new Ship();
        ship.setShipType(ShipType.DESTROYER);
        ship.setHits(1); // size=2, one more hit sinks it

        Cell cell = new Cell();
        cell.setHasShip(true);
        cell.setHit(false);
        cell.setShip(ship);

        // Other ship still alive
        Ship otherShip = new Ship();
        otherShip.setShipType(ShipType.CARRIER);
        otherShip.setHits(0);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(boardRepository.findByGameIdAndOwnerId(gameId, defender.getId())).thenReturn(Optional.of(targetBoard));
        when(cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), 5, 5)).thenReturn(Optional.of(cell));
        when(shipRepository.findAllByBoardId(targetBoard.getId())).thenReturn(List.of(ship, otherShip));

        ShotResultResponse result = shotService.processShot(gameId, attacker.getId(), 5, 5);

        assertThat(result.result()).isEqualTo(ShotResult.SUNK);
        assertThat(result.sunkShipType()).isEqualTo(ShipType.DESTROYER);
    }

    @Test
    void victory_shouldSetGameFinished() {
        Ship ship = new Ship();
        ship.setShipType(ShipType.DESTROYER);
        ship.setHits(1); // last hit sinks last ship

        Cell cell = new Cell();
        cell.setHasShip(true);
        cell.setHit(false);
        cell.setShip(ship);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(boardRepository.findByGameIdAndOwnerId(gameId, defender.getId())).thenReturn(Optional.of(targetBoard));
        when(cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), 0, 0)).thenReturn(Optional.of(cell));
        when(shipRepository.findAllByBoardId(targetBoard.getId())).thenReturn(List.of(ship));

        ShotResultResponse result = shotService.processShot(gameId, attacker.getId(), 0, 0);

        assertThat(result.result()).isEqualTo(ShotResult.SUNK);
        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(game.getWinner()).isEqualTo(attacker);
    }

    @Test
    void notYourTurn_shouldThrow() {
        game.setCurrentTurn(defender);
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> shotService.processShot(gameId, attacker.getId(), 0, 0))
            .isInstanceOf(NotYourTurnException.class);
    }

    @Test
    void cellAlreadyAttacked_shouldThrow() {
        Cell cell = new Cell();
        cell.setHit(true);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(boardRepository.findByGameIdAndOwnerId(gameId, defender.getId())).thenReturn(Optional.of(targetBoard));
        when(cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), 0, 0)).thenReturn(Optional.of(cell));

        assertThatThrownBy(() -> shotService.processShot(gameId, attacker.getId(), 0, 0))
            .isInstanceOf(CellAlreadyAttackedException.class);
    }

    @Test
    void gameNotInProgress_shouldThrow() {
        game.setStatus(GameStatus.FINISHED);
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> shotService.processShot(gameId, attacker.getId(), 0, 0))
            .isInstanceOf(GameNotInProgressException.class);
    }

    @Test
    void turnAlternatesAfterShot() {
        Cell cell = new Cell();
        cell.setHasShip(false);
        cell.setHit(false);

        Ship aliveShip = new Ship();
        aliveShip.setShipType(ShipType.CARRIER);
        aliveShip.setHits(0);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(boardRepository.findByGameIdAndOwnerId(gameId, defender.getId())).thenReturn(Optional.of(targetBoard));
        when(cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), 1, 1)).thenReturn(Optional.of(cell));
        when(shipRepository.findAllByBoardId(targetBoard.getId())).thenReturn(List.of(aliveShip));

        shotService.processShot(gameId, attacker.getId(), 1, 1);

        assertThat(game.getCurrentTurn()).isEqualTo(defender);
    }
}
