package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.response.AbilityResultResponse;
import com.softexpert.batalhanaval_api.dto.response.AbilityRotationResult;
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
    private final NotificationService notificationService;

    @Transactional
    public void initializeAbilities(Game game) {
        if (game.getGameMode() != GameMode.STORM) return;

        AbilityType[] types = AbilityType.values();

        AbilityType p1Ability = types[ThreadLocalRandom.current().nextInt(types.length)];
        AbilityType p2Ability = types[ThreadLocalRandom.current().nextInt(types.length)];

        createPlayerAbility(game, game.getPlayer1(), p1Ability);
        createPlayerAbility(game, game.getPlayer2(), p2Ability);

        game.setNextAbilityRotationTurn(game.getCurrentTurnNumber() + 3);

        log.info("Abilities initialized: game={}, p1={}, p2={}, nextRotation={}",
            game.getId(), p1Ability, p2Ability, game.getNextAbilityRotationTurn());
    }

    @Transactional
    public List<AbilityRotationResult> rotateAbilities(Game game) {
        List<PlayerAbility> abilities = playerAbilityRepository.findByGameId(game.getId());
        List<AbilityRotationResult> results = new ArrayList<>();

        AbilityType[] types = AbilityType.values();

        for (PlayerAbility ability : abilities) {
            AbilityType oldType = ability.getAbilityType();
            boolean wasDiscarded = !ability.isUsed();

            AbilityType newType = types[ThreadLocalRandom.current().nextInt(types.length)];

            ability.setAbilityType(newType);
            ability.setUsed(false);
            ability.setUsedOnTurn(null);
            playerAbilityRepository.save(ability);

            results.add(new AbilityRotationResult(
                ability.getUser().getId(),
                newType,
                newType.getDisplayName(),
                newType.getDescription(),
                oldType,
                wasDiscarded
            ));
        }

        game.setNextAbilityRotationTurn(game.getCurrentTurnNumber() + 3);

        log.info("Abilities rotated: game={}, turn={}, nextRotation={}, results={}",
            game.getId(), game.getCurrentTurnNumber(), game.getNextAbilityRotationTurn(), results);

        return results;
    }

    @Transactional
    public AbilityResultResponse useAbility(UUID gameId, UUID userId, AbilityType requestedType, Integer row, Integer col, String axis, Integer index) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);

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

        AbilityResultResponse result = switch (requestedType) {
            case RADAR -> executeRadar(game, userId, row, col);
            case DOUBLE_TORPEDO -> executeDoubleTorpedo(game, userId, row, col);
            case SHIELD -> executeShield(game, userId);
            case LINE_BOMBARDMENT -> executeLineBombardment(game, userId, axis, index);
        };

        ability.setUsed(true);
        ability.setUsedOnTurn(game.getCurrentTurnNumber());
        playerAbilityRepository.save(ability);

        if (game.getStatus() == GameStatus.IN_PROGRESS) {
            advanceTurnAfterAbility(game, userId);
            gameRepository.save(game);
        }

        log.info("Ability used: game={}, player={}, type={}", gameId, userId, requestedType);

        return result;
    }

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
        UUID defenderId = getDefenderId(game, userId);
        Board targetBoard = boardRepository.findByGameIdAndOwnerId(game.getId(), defenderId).orElseThrow();

        ShotResultResponse shot1 = processSingleShot(game, userId, targetBoard, row, col);

        if (game.getStatus() == GameStatus.FINISHED) {
            return AbilityResultResponse.doubleTorpedo(List.of(shot1));
        }

        int secondRow = row + 1 <= 9 ? row + 1 : row - 1;
        ShotResultResponse shot2 = processSingleShot(game, userId, targetBoard, secondRow, col);

        List<ShotResultResponse> results = List.of(shot1, shot2);
        return AbilityResultResponse.doubleTorpedo(results);
    }

    private AbilityResultResponse executeShield(Game game, UUID userId) {
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
            if (cell == null || cell.isHit()) continue;

            ShotResultResponse shotResult = processSingleShot(game, userId, targetBoard, r, c);
            results.add(shotResult);

            if (game.getStatus() == GameStatus.FINISHED) {
                break;
            }
        }

        return AbilityResultResponse.lineBombardment(results);
    }

    private ShotResultResponse processSingleShot(Game game, UUID userId, Board targetBoard, int row, int col) {
        Cell cell = cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), row, col).orElse(null);

        if (cell == null || cell.isHit()) {
            return new ShotResultResponse(game.getId(), row, col, ShotResult.MISS, null, null, null, null);
        }

        cell.setHit(true);

        ShotResult result;
        ShipType sunkShipType = null;
        Integer sunkShipOriginRow = null;
        Integer sunkShipOriginCol = null;
        Orientation sunkShipOrientation = null;

        if (cell.isHasShip()) {
            Ship ship = cell.getShip();
            ship.setHits(ship.getHits() + 1);
            shipRepository.saveAndFlush(ship);
            if (ship.isSunk()) {
                result = ShotResult.SUNK;
                sunkShipType = ship.getShipType();
                sunkShipOriginRow = ship.getOriginRow();
                sunkShipOriginCol = ship.getOriginCol();
                sunkShipOrientation = ship.getOrientation();
            } else {
                result = ShotResult.HIT;
            }
        } else {
            result = ShotResult.MISS;
        }

        Shot shot = new Shot();
        shot.setGame(game);
        shot.setAttacker(game.getCurrentTurn());
        shot.setTargetBoard(targetBoard);
        shot.setRow(row);
        shot.setCol(col);
        shot.setResult(result);
        shot.setSunkShipType(sunkShipType);
        shot.setSunkShipOriginRow(sunkShipOriginRow);
        shot.setSunkShipOriginCol(sunkShipOriginCol);
        shot.setSunkShipOrientation(sunkShipOrientation);
        shotRepository.save(shot);

        if (result == ShotResult.SUNK) {
            victoryService.checkVictoryCondition(game, userId, targetBoard);
        }

        return new ShotResultResponse(game.getId(), row, col, result, sunkShipType, sunkShipOriginRow, sunkShipOriginCol, sunkShipOrientation);
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

    public boolean hasActiveShield(UUID gameId, UUID defenderId) {
        return playerAbilityRepository.findByGameIdAndUserId(gameId, defenderId)
            .filter(a -> a.getAbilityType() == AbilityType.SHIELD)
            .filter(PlayerAbility::isUsed)
            .isPresent();
    }

    @Transactional
    public void consumeShield(UUID gameId, UUID defenderId) {
        playerAbilityRepository.findByGameIdAndUserId(gameId, defenderId)
            .ifPresent(ability -> {
                ability.setUsedOnTurn(-1);
                playerAbilityRepository.save(ability);
            });
    }

    public boolean isShieldActiveAndNotConsumed(UUID gameId, UUID defenderId) {
        return playerAbilityRepository.findByGameIdAndUserId(gameId, defenderId)
            .filter(a -> a.getAbilityType() == AbilityType.SHIELD)
            .filter(PlayerAbility::isUsed)
            .filter(a -> a.getUsedOnTurn() != null && a.getUsedOnTurn() >= 0)
            .isPresent();
    }

    private void advanceTurnAfterAbility(Game game, UUID userId) {
        if (game.isBonusShot()) {
            game.setBonusShot(false);
            return;
        }

        User nextTurn = game.getPlayer1().getId().equals(userId)
            ? game.getPlayer2()
            : game.getPlayer1();
        game.setCurrentTurn(nextTurn);

        game.setCurrentTurnNumber(game.getCurrentTurnNumber() + 1);

        stormService.clearExpiredEffects(game);

        if (game.getGameMode() == GameMode.STORM && game.getCurrentTurnNumber() == game.getNextStormTurn()) {
            stormService.generateNextStormEvent(game.getId());
        }

        if (game.getGameMode() == GameMode.STORM && game.getCurrentTurnNumber() == game.getNextAbilityRotationTurn()) {
            List<AbilityRotationResult> rotationResults = rotateAbilities(game);
            notificationService.notifyAbilityRotated(rotationResults);
        }
    }
}
