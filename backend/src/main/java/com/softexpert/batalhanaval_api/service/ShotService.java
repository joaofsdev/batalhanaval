package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.response.ShotResultResponse;
import com.softexpert.batalhanaval_api.exception.*;
import com.softexpert.batalhanaval_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public ShotResultResponse processShot(UUID gameId, UUID attackerId, int row, int col) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameNotInProgressException();
        }
        if (!game.getCurrentTurn().getId().equals(attackerId)) {
            throw new NotYourTurnException();
        }

        // Storm mode: check if TIDE blocks this row
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

        // Storm mode: check if defender has active shield
        if (game.getGameMode() == GameMode.STORM && abilityService.isShieldActiveAndNotConsumed(gameId, defenderId)) {
            // Shield absorbs the shot entirely — cell is NOT marked as hit
            // No Shot record is saved so the cell remains attackable in future turns
            abilityService.consumeShield(gameId, defenderId);

            advanceTurn(game, attackerId);
            gameRepository.save(game);

            return new ShotResultResponse(gameId, row, col, ShotResult.MISS, null, null, null, null);
        }

        // Normal shot processing
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

        // Check victory
        boolean allSunk = victoryService.checkVictoryCondition(game, attackerId, targetBoard);
        if (allSunk) {
            return new ShotResultResponse(gameId, row, col, result, sunkShipType, sunkShipOriginRow, sunkShipOriginCol, sunkShipOrientation);
        }

        // Capture fog state before advancing (advanceTurn may clear expired fog)
        boolean fogActiveForThisShot = game.getGameMode() == GameMode.STORM && game.isFogActive();

        // Advance turn
        advanceTurn(game, attackerId);
        gameRepository.save(game);

        // Fog: mask the result from the attacker (shot is still processed normally)
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

    /**
     * Advance turn logic. Handles bonus shot (CALM) and turn number increment.
     */
    private void advanceTurn(Game game, UUID attackerId) {
        game.setConsecutiveSkips(0);

        // If bonus shot is active, consume it instead of switching turn
        if (game.isBonusShot()) {
            game.setBonusShot(false);
            // Player keeps turn (bonus shot consumed)
            return;
        }

        // Alternate turn
        User nextTurn = game.getPlayer1().getId().equals(attackerId)
            ? game.getPlayer2()
            : game.getPlayer1();
        game.setCurrentTurn(nextTurn);

        // Increment turn number (each full round = 1 turn for counting storm)
        game.setCurrentTurnNumber(game.getCurrentTurnNumber() + 1);

        // Clear expired storm effects (fog, tide last 2 turns)
        if (game.getGameMode() == GameMode.STORM) {
            stormService.clearExpiredEffects(game);
        }

        // Storm mode: check if we need to generate next storm event
        if (game.getGameMode() == GameMode.STORM && game.getCurrentTurnNumber() == game.getNextStormTurn()) {
            stormService.generateNextStormEvent(game.getId());
        }
    }
}
