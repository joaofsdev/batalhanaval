package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.response.AbilityResultResponse;
import com.softexpert.batalhanaval_api.dto.response.ShotResultResponse;
import com.softexpert.batalhanaval_api.exception.*;
import com.softexpert.batalhanaval_api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AbilityService {

    private final GameRepository gameRepository;
    private final PlayerAbilityRepository playerAbilityRepository;
    private final BoardRepository boardRepository;
    private final CellRepository cellRepository;
    private final ShipRepository shipRepository;
    private final ShotRepository shotRepository;
    private final StormService stormService;
    private final VictoryService victoryService;

    /**
     * Initialize abilities for both players when a STORM mode game starts.
     * Assigns 1 random ability per player.
     */
    @Transactional
    public void initializeAbilities(Game game) {
        if (game.getGameMode() != GameMode.STORM) return;

        AbilityType[] types = AbilityType.values();

        AbilityType p1Ability = types[ThreadLocalRandom.current().nextInt(types.length)];
        AbilityType p2Ability = types[ThreadLocalRandom.current().nextInt(types.length)];

        createPlayerAbility(game, game.getPlayer1(), p1Ability);
        createPlayerAbility(game, game.getPlayer2(), p2Ability);

        log.info("Abilities initialized: game={}, p1={}, p2={}", game.getId(), p1Ability, p2Ability);
    }

    /**
     * Use an ability. Validates all preconditions and executes the effect.
     */
    @Transactional
    public AbilityResultResponse useAbility(UUID gameId, UUID userId, AbilityType requestedType, Integer row, Integer col, String axis, Integer index) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);

        // Validations
        if (game.getGameMode() != GameMode.STORM) {
            throw new NotStormModeException();
        }
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameNotInProgressException();
        }
        if (!game.getCurrentTurn().getId().equals(userId)) {
            throw new NotYourTurnException();
        }
        if (stormService.isStormTurn(gameId, game.getCurrentTurnNumber())) {
            throw new AbilityBlockedByStormException();
        }

        PlayerAbility ability = playerAbilityRepository.findByGameIdAndUserId(gameId, userId)
            .orElseThrow(GameNotFoundException::new);

        if (ability.isUsed()) {
            throw new AbilityAlreadyUsedException();
        }
        if (ability.getAbilityType() != requestedType) {
            throw new InvalidAbilityTypeException();
        }

        // Execute ability
        AbilityResultResponse result = switch (requestedType) {
            case RADAR -> executeRadar(game, userId, row, col);
            case DOUBLE_TORPEDO -> executeDoubleTorpedo(game, userId, row, col);
            case SHIELD -> executeShield(game, userId);
            case LINE_BOMBARDMENT -> executeLineBombardment(game, userId, axis, index);
        };

        // Mark as used
        ability.setUsed(true);
        ability.setUsedOnTurn(game.getCurrentTurnNumber());
        playerAbilityRepository.save(ability);

        // Advance turn (ability consumes the player's turn) — unless game ended
        if (game.getStatus() == GameStatus.IN_PROGRESS) {
            advanceTurnAfterAbility(game, userId);
            gameRepository.save(game);
        }

        log.info("Ability used: game={}, player={}, type={}", gameId, userId, requestedType);

        return result;
    }

    // --- Ability Executions ---

    private AbilityResultResponse executeRadar(Game game, UUID userId, Integer centerRow, Integer centerCol) {
        UUID defenderId = getDefenderId(game, userId);
        Board targetBoard = boardRepository.findByGameIdAndOwnerId(game.getId(), defenderId).orElseThrow();

        boolean[][] grid = new boolean[3][3];
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int r = centerRow + dr;
                int c = centerCol + dc;
                if (r >= 0 && r <= 9 && c >= 0 && c <= 9) {
                    grid[dr + 1][dc + 1] = cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), r, c)
                        .map(Cell::isHasShip)
                        .orElse(false);
                }
            }
        }

        return AbilityResultResponse.radar(grid, centerRow, centerCol);
    }

    private AbilityResultResponse executeDoubleTorpedo(Game game, UUID userId, Integer row, Integer col) {
        // First shot at given coords, second shot at adjacent cell (row+1 or col+1)
        UUID defenderId = getDefenderId(game, userId);
        Board targetBoard = boardRepository.findByGameIdAndOwnerId(game.getId(), defenderId).orElseThrow();

        ShotResultResponse shot1 = processSingleShot(game, userId, targetBoard, row, col);

        // If first shot caused victory, skip second shot
        if (game.getStatus() == GameStatus.FINISHED) {
            return AbilityResultResponse.doubleTorpedo(List.of(shot1));
        }

        // Second shot: try row+1, if out of bounds try row-1
        int secondRow = row + 1 <= 9 ? row + 1 : row - 1;
        ShotResultResponse shot2 = processSingleShot(game, userId, targetBoard, secondRow, col);

        List<ShotResultResponse> results = List.of(shot1, shot2);
        return AbilityResultResponse.doubleTorpedo(results);
    }

    private AbilityResultResponse executeShield(Game game, UUID userId) {
        // Shield is stored as a flag on PlayerAbility — when processing incoming shots,
        // check if defender has an active shield (used=true but shield effect pending).
        // We'll add a 'shieldActive' field approach: use the board owner's ability record.
        // For simplicity, shield effect is tracked by checking if the ability was SHIELD and used=true
        // The ShotService will check for shield before applying damage.
        return AbilityResultResponse.shield();
    }

    private AbilityResultResponse executeLineBombardment(Game game, UUID userId, String axis, Integer index) {
        UUID defenderId = getDefenderId(game, userId);
        Board targetBoard = boardRepository.findByGameIdAndOwnerId(game.getId(), defenderId).orElseThrow();

        List<ShotResultResponse> results = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            int r = "ROW".equalsIgnoreCase(axis) ? index : i;
            int c = "COL".equalsIgnoreCase(axis) ? index : i;

            Cell cell = cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), r, c).orElse(null);
            if (cell == null || cell.isHit()) continue; // Skip already hit cells

            ShotResultResponse shotResult = processSingleShot(game, userId, targetBoard, r, c);
            results.add(shotResult);

            // If victory was achieved, stop processing remaining cells
            if (game.getStatus() == GameStatus.FINISHED) {
                break;
            }
        }

        return AbilityResultResponse.lineBombardment(results);
    }

    // --- Helper Methods ---

    /**
     * Process a single shot without turn switching.
     * Checks victory condition after sinking a ship.
     * Used by DOUBLE_TORPEDO and LINE_BOMBARDMENT.
     *
     * @return the shot result (check gameFinished flag via game status after call)
     */
    private ShotResultResponse processSingleShot(Game game, UUID userId, Board targetBoard, int row, int col) {
        Cell cell = cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), row, col).orElse(null);

        if (cell == null || cell.isHit()) {
            // Already hit or invalid — return miss for this position
            return new ShotResultResponse(game.getId(), row, col, ShotResult.MISS, null);
        }

        cell.setHit(true);

        ShotResult result;
        ShipType sunkShipType = null;

        if (cell.isHasShip()) {
            Ship ship = cell.getShip();
            ship.setHits(ship.getHits() + 1);
            shipRepository.saveAndFlush(ship);
            if (ship.isSunk()) {
                result = ShotResult.SUNK;
                sunkShipType = ship.getShipType();
            } else {
                result = ShotResult.HIT;
            }
        } else {
            result = ShotResult.MISS;
        }

        // Record shot
        Shot shot = new Shot();
        shot.setGame(game);
        shot.setAttacker(game.getCurrentTurn());
        shot.setTargetBoard(targetBoard);
        shot.setRow(row);
        shot.setCol(col);
        shot.setResult(result);
        shot.setSunkShipType(sunkShipType);
        shotRepository.save(shot);

        // Check victory condition after sinking a ship
        if (result == ShotResult.SUNK) {
            victoryService.checkVictoryCondition(game, userId, targetBoard);
        }

        return new ShotResultResponse(game.getId(), row, col, result, sunkShipType);
    }

    private void createPlayerAbility(Game game, User player, AbilityType type) {
        PlayerAbility ability = new PlayerAbility();
        ability.setGame(game);
        ability.setUser(player);
        ability.setAbilityType(type);
        ability.setUsed(false);
        playerAbilityRepository.save(ability);
    }

    private UUID getDefenderId(Game game, UUID attackerId) {
        return game.getPlayer1().getId().equals(attackerId)
            ? game.getPlayer2().getId()
            : game.getPlayer1().getId();
    }

    /**
     * Check if a player has an active shield (SHIELD ability used but not yet consumed).
     * Called by ShotService when processing incoming shots.
     */
    public boolean hasActiveShield(UUID gameId, UUID defenderId) {
        return playerAbilityRepository.findByGameIdAndUserId(gameId, defenderId)
            .filter(a -> a.getAbilityType() == AbilityType.SHIELD)
            .filter(PlayerAbility::isUsed)
            .isPresent();
    }

    /**
     * Consume the shield after blocking a shot. Marks it as no longer active.
     * We track "shield consumed" by setting usedOnTurn to a negative value (convention).
     */
    @Transactional
    public void consumeShield(UUID gameId, UUID defenderId) {
        playerAbilityRepository.findByGameIdAndUserId(gameId, defenderId)
            .ifPresent(ability -> {
                // Mark shield as consumed by setting usedOnTurn to -1 (consumed flag)
                ability.setUsedOnTurn(-1);
                playerAbilityRepository.save(ability);
            });
    }

    /**
     * Check if shield is still active (used but not consumed).
     */
    public boolean isShieldActiveAndNotConsumed(UUID gameId, UUID defenderId) {
        return playerAbilityRepository.findByGameIdAndUserId(gameId, defenderId)
            .filter(a -> a.getAbilityType() == AbilityType.SHIELD)
            .filter(PlayerAbility::isUsed)
            .filter(a -> a.getUsedOnTurn() != null && a.getUsedOnTurn() >= 0)
            .isPresent();
    }

    /**
     * Advance turn after ability use. Mirrors ShotService.advanceTurn logic.
     * Ability usage consumes the player's turn.
     */
    private void advanceTurnAfterAbility(Game game, UUID userId) {
        // If bonus shot is active, consume it instead of switching turn
        if (game.isBonusShot()) {
            game.setBonusShot(false);
            return;
        }

        // Alternate turn
        User nextTurn = game.getPlayer1().getId().equals(userId)
            ? game.getPlayer2()
            : game.getPlayer1();
        game.setCurrentTurn(nextTurn);

        // Increment turn number
        game.setCurrentTurnNumber(game.getCurrentTurnNumber() + 1);

        // Clear expired storm effects (fog, tide last 2 turns)
        stormService.clearExpiredEffects(game);

        // Storm mode: check if we need to generate next storm event
        if (game.getGameMode() == GameMode.STORM && game.getCurrentTurnNumber() == game.getNextStormTurn()) {
            stormService.generateNextStormEvent(game.getId());
        }
    }
}
