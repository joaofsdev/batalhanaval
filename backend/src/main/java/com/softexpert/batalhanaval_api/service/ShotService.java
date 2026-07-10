package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.response.AbilityRotationResult;
import com.softexpert.batalhanaval_api.dto.response.ShotResultResponse;
import com.softexpert.batalhanaval_api.exception.*;
import com.softexpert.batalhanaval_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShotService {

    private final GameRepository gameRepository;
    private final BoardRepository boardRepository;
    private final CellRepository cellRepository;
    private final ShipRepository shipRepository;
    private final ShotRepository shotRepository;
    private final StormService stormService;
    private final AbilityService abilityService;
    private final VictoryService victoryService;
    private final NotificationService notificationService;

    @Transactional
    public ShotResultResponse processShot(UUID gameId, UUID attackerId, int row, int col) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameNotInProgressException();
        }
        if (!game.getCurrentTurn().getId().equals(attackerId)) {
            throw new NotYourTurnException();
        }

        if (game.getGameMode() == GameMode.STORM) {
            if (stormService.isShotBlockedByTide(gameId, row)) {
                throw new StormBlocksShotException("Maré Alta! Linha " + row + " está bloqueada neste turno.");
            }
        }

        UUID defenderId = game.getPlayer1().getId().equals(attackerId)
            ? game.getPlayer2().getId()
            : game.getPlayer1().getId();

        Board targetBoard = boardRepository.findByGameIdAndOwnerId(gameId, defenderId)
            .orElseThrow(GameNotFoundException::new);

        Cell cell = cellRepository.findByBoardIdAndRowAndCol(targetBoard.getId(), row, col)
            .orElseThrow(() -> new InvalidShipPlacementException("Invalid coordinates", "INVALID_COORDINATES"));

        if (cell.isHit()) {
            throw new CellAlreadyAttackedException();
        }

        if (game.getGameMode() == GameMode.STORM && abilityService.isShieldActiveAndNotConsumed(gameId, defenderId)) {
            abilityService.consumeShield(gameId, defenderId);

            advanceTurn(game, attackerId);
            gameRepository.save(game);

            return new ShotResultResponse(gameId, row, col, ShotResult.MISS, null, null, null, null);
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

        boolean allSunk = victoryService.checkVictoryCondition(game, attackerId, targetBoard);
        if (allSunk) {
            return new ShotResultResponse(gameId, row, col, result, sunkShipType, sunkShipOriginRow, sunkShipOriginCol, sunkShipOrientation);
        }

        boolean fogActiveForThisShot = game.getGameMode() == GameMode.STORM && game.isFogActive();

        advanceTurn(game, attackerId);
        gameRepository.save(game);

        if (fogActiveForThisShot) {
            return new ShotResultResponse(gameId, row, col, ShotResult.HIDDEN, null, null, null, null);
        }

        return new ShotResultResponse(gameId, row, col, result, sunkShipType, sunkShipOriginRow, sunkShipOriginCol, sunkShipOrientation);
    }

    public UUID getDefenderId(Game game, UUID attackerId) {
        return game.getPlayer1().getId().equals(attackerId)
            ? game.getPlayer2().getId()
            : game.getPlayer1().getId();
    }

    private void advanceTurn(Game game, UUID attackerId) {
        game.setConsecutiveSkips(0);

        if (game.isBonusShot()) {
            game.setBonusShot(false);
            return;
        }

        User nextTurn = game.getPlayer1().getId().equals(attackerId)
            ? game.getPlayer2()
            : game.getPlayer1();
        game.setCurrentTurn(nextTurn);

        game.setCurrentTurnNumber(game.getCurrentTurnNumber() + 1);

        if (game.getGameMode() == GameMode.STORM) {
            stormService.clearExpiredEffects(game);
        }

        if (game.getGameMode() == GameMode.STORM && game.getCurrentTurnNumber() == game.getNextStormTurn()) {
            stormService.generateNextStormEvent(game.getId());
        }

        if (game.getGameMode() == GameMode.STORM && game.getCurrentTurnNumber() == game.getNextAbilityRotationTurn()) {
            List<AbilityRotationResult> rotationResults = abilityService.rotateAbilities(game);
            notificationService.notifyAbilityRotated(rotationResults);
        }
    }
}
