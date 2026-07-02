package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.response.AbilityResultResponse;
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
class AbilityServiceTest {

    @Mock private GameRepository gameRepository;
    @Mock private PlayerAbilityRepository playerAbilityRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private CellRepository cellRepository;
    @Mock private ShipRepository shipRepository;
    @Mock private ShotRepository shotRepository;
    @Mock private StormService stormService;
    @Mock private VictoryService victoryService;

    @InjectMocks private AbilityService abilityService;

    private Game game;
    private UUID gameId;
    private User player1;
    private User player2;
    private Board targetBoard;

    @BeforeEach
    void setUp() {
        gameId = UUID.randomUUID();
        player1 = new User();
        player1.setId(UUID.randomUUID());
        player2 = new User();
        player2.setId(UUID.randomUUID());

        game = new Game();
        game.setId(gameId);
        game.setPlayer1(player1);
        game.setPlayer2(player2);
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setGameMode(GameMode.STORM);
        game.setCurrentTurn(player1);
        game.setCurrentTurnNumber(5);

        targetBoard = new Board();
        targetBoard.setId(UUID.randomUUID());
        targetBoard.setOwner(player2);
    }

    @Test
    void radar_returnsBooleanGridOnly_neverShipType() {
        PlayerAbility ability = createAbility(player1, AbilityType.RADAR, false);

        // Setup 3x3 area centered on (5,5) — cells (4,4) to (6,6)
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(stormService.isStormTurn(gameId, 5)).thenReturn(false);
        when(playerAbilityRepository.findByGameIdAndUserId(gameId, player1.getId()))
            .thenReturn(Optional.of(ability));
        when(boardRepository.findByGameIdAndOwnerId(gameId, player2.getId()))
            .thenReturn(Optional.of(targetBoard));

        // Cell (4,4) has ship, others don't
        Cell cellWithShip = new Cell();
        cellWithShip.setHasShip(true);
        Ship ship = new Ship();
        ship.setShipType(ShipType.CARRIER);
        cellWithShip.setShip(ship);

        Cell emptyCell = new Cell();
        emptyCell.setHasShip(false);

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int r = 5 + dr;
                int c = 5 + dc;
                if (r == 4 && c == 4) {
                    when(cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), r, c))
                        .thenReturn(Optional.of(cellWithShip));
                } else {
                    Cell cell = new Cell();
                    cell.setHasShip(false);
                    when(cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), r, c))
                        .thenReturn(Optional.of(cell));
                }
            }
        }

        AbilityResultResponse result = abilityService.useAbility(
            gameId, player1.getId(), AbilityType.RADAR, 5, 5, null, null);

        // Returns boolean[][] — only true/false, never ship type info
        assertThat(result.abilityType()).isEqualTo(AbilityType.RADAR);
        assertThat(result.radarGrid()).isNotNull();
        assertThat(result.radarGrid()[0][0]).isTrue();  // (4,4) has ship
        assertThat(result.radarGrid()[1][1]).isFalse(); // (5,5) no ship

        // Response contains ONLY boolean grid — no ship type exposed
        assertThat(result.shotResults()).isNull();
        assertThat(result.centerRow()).isEqualTo(5);
        assertThat(result.centerCol()).isEqualTo(5);
    }

    @Test
    void lineBombardment_returnsOnlyHitCells_noFullBoardExposure() {
        PlayerAbility ability = createAbility(player1, AbilityType.LINE_BOMBARDMENT, false);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(stormService.isStormTurn(gameId, 5)).thenReturn(false);
        when(playerAbilityRepository.findByGameIdAndUserId(gameId, player1.getId()))
            .thenReturn(Optional.of(ability));
        when(boardRepository.findByGameIdAndOwnerId(gameId, player2.getId()))
            .thenReturn(Optional.of(targetBoard));

        // Row 3: cells 0-9. Cell 3 has ship, cell 7 already hit, rest empty
        for (int c = 0; c < 10; c++) {
            Cell cell = new Cell();
            if (c == 3) {
                cell.setHasShip(true);
                cell.setHit(false);
                Ship ship = new Ship();
                ship.setId(UUID.randomUUID());
                ship.setShipType(ShipType.CRUISER);
                ship.setHits(0);
                cell.setShip(ship);
                when(shipRepository.saveAndFlush(ship)).thenReturn(ship);
            } else if (c == 7) {
                cell.setHit(true); // already attacked — should be skipped
                cell.setHasShip(false);
            } else {
                cell.setHasShip(false);
                cell.setHit(false);
            }
            when(cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), 3, c))
                .thenReturn(Optional.of(cell));
        }

        AbilityResultResponse result = abilityService.useAbility(
            gameId, player1.getId(), AbilityType.LINE_BOMBARDMENT, null, null, "ROW", 3);

        assertThat(result.abilityType()).isEqualTo(AbilityType.LINE_BOMBARDMENT);
        assertThat(result.shotResults()).isNotNull();
        // Returns only cells that were actually attacked (9 new cells, skipping the already-hit one)
        assertThat(result.shotResults()).hasSize(9);
        // Does NOT expose full board state — only per-cell results
        assertThat(result.radarGrid()).isNull();
    }

    @Test
    void shield_blocksExactlyOneShot() {
        PlayerAbility ability = createAbility(player1, AbilityType.SHIELD, false);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(stormService.isStormTurn(gameId, 5)).thenReturn(false);
        when(playerAbilityRepository.findByGameIdAndUserId(gameId, player1.getId()))
            .thenReturn(Optional.of(ability));

        AbilityResultResponse result = abilityService.useAbility(
            gameId, player1.getId(), AbilityType.SHIELD, null, null, null, null);

        assertThat(result.abilityType()).isEqualTo(AbilityType.SHIELD);
        assertThat(ability.isUsed()).isTrue();

        // After shield activation, isShieldActiveAndNotConsumed should return true
        // Then after consumeShield, it should return false (blocking exactly 1 shot)
        when(playerAbilityRepository.findByGameIdAndUserId(gameId, player1.getId()))
            .thenReturn(Optional.of(ability));

        // Shield active: used=true, usedOnTurn >= 0
        assertThat(ability.getUsedOnTurn()).isEqualTo(5);
        boolean active = abilityService.isShieldActiveAndNotConsumed(gameId, player1.getId());
        assertThat(active).isTrue();

        // Consume shield (simulates blocking 1 shot)
        abilityService.consumeShield(gameId, player1.getId());
        assertThat(ability.getUsedOnTurn()).isEqualTo(-1); // consumed marker

        // After consumption, shield is no longer active
        boolean activeAfter = abilityService.isShieldActiveAndNotConsumed(gameId, player1.getId());
        assertThat(activeAfter).isFalse();
    }

    @Test
    void doubleTorpedo_countsAsOneTurn_notTwo() {
        PlayerAbility ability = createAbility(player1, AbilityType.DOUBLE_TORPEDO, false);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(stormService.isStormTurn(gameId, 5)).thenReturn(false);
        when(playerAbilityRepository.findByGameIdAndUserId(gameId, player1.getId()))
            .thenReturn(Optional.of(ability));
        when(boardRepository.findByGameIdAndOwnerId(gameId, player2.getId()))
            .thenReturn(Optional.of(targetBoard));

        // Two cells for the two torpedo shots
        Cell cell1 = new Cell();
        cell1.setHasShip(false);
        cell1.setHit(false);

        Cell cell2 = new Cell();
        cell2.setHasShip(false);
        cell2.setHit(false);

        when(cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), 4, 4))
            .thenReturn(Optional.of(cell1));
        when(cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), 5, 4))
            .thenReturn(Optional.of(cell2));

        int turnBefore = game.getCurrentTurnNumber();

        AbilityResultResponse result = abilityService.useAbility(
            gameId, player1.getId(), AbilityType.DOUBLE_TORPEDO, 4, 4, null, null);

        // Both shots in one ability call (single turn)
        assertThat(result.abilityType()).isEqualTo(AbilityType.DOUBLE_TORPEDO);
        assertThat(result.shotResults()).hasSize(2);

        // Turn number did NOT increment (ability doesn't advance turn)
        assertThat(game.getCurrentTurnNumber()).isEqualTo(turnBefore);

        // The ability is marked as used on the same turn
        assertThat(ability.getUsedOnTurn()).isEqualTo(5);
    }

    @Test
    void abilityAlreadyUsed_shouldThrow() {
        PlayerAbility ability = createAbility(player1, AbilityType.RADAR, true);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(stormService.isStormTurn(gameId, 5)).thenReturn(false);
        when(playerAbilityRepository.findByGameIdAndUserId(gameId, player1.getId()))
            .thenReturn(Optional.of(ability));

        assertThatThrownBy(() -> abilityService.useAbility(
            gameId, player1.getId(), AbilityType.RADAR, 5, 5, null, null))
            .isInstanceOf(AbilityAlreadyUsedException.class);
    }

    @Test
    void playerCannotUseDifferentAbilityType_shouldThrow() {
        // Player has RADAR but tries to use SHIELD
        PlayerAbility ability = createAbility(player1, AbilityType.RADAR, false);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(stormService.isStormTurn(gameId, 5)).thenReturn(false);
        when(playerAbilityRepository.findByGameIdAndUserId(gameId, player1.getId()))
            .thenReturn(Optional.of(ability));

        assertThatThrownBy(() -> abilityService.useAbility(
            gameId, player1.getId(), AbilityType.SHIELD, null, null, null, null))
            .isInstanceOf(InvalidAbilityTypeException.class);
    }

    @Test
    void stormTurn_blocksAbilityUse_shouldThrow() {
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(stormService.isStormTurn(gameId, 5)).thenReturn(true);

        assertThatThrownBy(() -> abilityService.useAbility(
            gameId, player1.getId(), AbilityType.RADAR, 5, 5, null, null))
            .isInstanceOf(AbilityBlockedByStormException.class);
    }

    @Test
    void lineBombardment_sinksLastShipMidSequence_shouldFinishGameAndStopProcessing() {
        PlayerAbility ability = createAbility(player1, AbilityType.LINE_BOMBARDMENT, false);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(stormService.isStormTurn(gameId, 5)).thenReturn(false);
        when(playerAbilityRepository.findByGameIdAndUserId(gameId, player1.getId()))
            .thenReturn(Optional.of(ability));
        when(boardRepository.findByGameIdAndOwnerId(gameId, player2.getId()))
            .thenReturn(Optional.of(targetBoard));

        // Setup: only 1 ship left (DESTROYER size 2, already hit once — 1 more hit = sunk)
        Ship lastShip = new Ship();
        lastShip.setId(UUID.randomUUID());
        lastShip.setShipType(ShipType.DESTROYER); // size 2
        lastShip.setHits(1); // one more hit → sunk

        // Row 2, cols 0-9:
        // Col 0: empty, not hit (will be processed → MISS)
        // Col 1: empty, not hit (will be processed → MISS)
        // Col 2: empty, not hit (will be processed → MISS)
        // Col 3: has last ship, not hit (will be processed → SUNK → game finishes)
        // Col 4+: should NOT be processed because game is finished
        for (int c = 0; c < 10; c++) {
            Cell cell = new Cell();
            cell.setHit(false);
            if (c == 3) {
                cell.setHasShip(true);
                cell.setShip(lastShip);
            } else {
                cell.setHasShip(false);
            }
            when(cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), 2, c))
                .thenReturn(Optional.of(cell));
        }

        when(shipRepository.saveAndFlush(lastShip)).thenReturn(lastShip);

        // After the ship is sunk, VictoryService detects all ships sunk → finishes game
        when(victoryService.checkVictoryCondition(eq(game), eq(player1.getId()), eq(targetBoard)))
            .thenAnswer(invocation -> {
                // Simulate VictoryService behavior: mark game as finished
                game.setStatus(GameStatus.FINISHED);
                game.setWinner(player1);
                game.setCurrentTurn(null);
                return true;
            });

        AbilityResultResponse result = abilityService.useAbility(
            gameId, player1.getId(), AbilityType.LINE_BOMBARDMENT, null, null, "ROW", 2);

        // Game should be FINISHED
        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(game.getWinner()).isEqualTo(player1);
        assertThat(game.getCurrentTurn()).isNull();

        // Should have processed exactly 4 cells (cols 0,1,2,3) — stopped at col 3 after victory
        assertThat(result.shotResults()).hasSize(4);

        // Last shot should be SUNK
        ShotResultResponse lastShotResult = result.shotResults().get(3);
        assertThat(lastShotResult.result()).isEqualTo(ShotResult.SUNK);
        assertThat(lastShotResult.sunkShipType()).isEqualTo(ShipType.DESTROYER);

        // Verify that cells 4-9 were NOT queried for processing (game stopped)
        // We verify indirectly: only 4 shots recorded
        verify(shotRepository, times(4)).save(any(Shot.class));
    }

    // --- Helper ---

    private PlayerAbility createAbility(User user, AbilityType type, boolean used) {
        PlayerAbility ability = new PlayerAbility();
        ability.setId(UUID.randomUUID());
        ability.setGame(game);
        ability.setUser(user);
        ability.setAbilityType(type);
        ability.setUsed(used);
        if (used) ability.setUsedOnTurn(3);
        return ability;
    }
}
