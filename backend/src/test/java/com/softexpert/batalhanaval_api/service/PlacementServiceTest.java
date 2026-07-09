package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.request.ShipPlacement;
import com.softexpert.batalhanaval_api.exception.InvalidShipPlacementException;
import com.softexpert.batalhanaval_api.repository.ShipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PlacementServiceTest {

    private PlacementService placementService;
    private Board board;

    @BeforeEach
    void setUp() {
        placementService = new PlacementService(mock(ShipRepository.class));
        board = new Board();
        board.setReady(false);
        board.setShips(new ArrayList<>());
        List<Cell> cells = new ArrayList<>();
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                Cell cell = new Cell();
                cell.setBoard(board);
                cell.setRow(r);
                cell.setCol(c);
                cell.setHasShip(false);
                cell.setHit(false);
                cells.add(cell);
            }
        }
        board.setCells(cells);
    }

    private List<ShipPlacement> validFleet() {
        return List.of(
            new ShipPlacement(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.BATTLESHIP, 2, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.CRUISER, 4, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.SUBMARINE, 6, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.DESTROYER, 8, 0, Orientation.HORIZONTAL)
        );
    }

    @Test
    void validFleet_shouldSucceed() {
        placementService.validateAndPlaceShips(board, validFleet());

        assertThat(board.isReady()).isTrue();
        assertThat(board.getShips()).hasSize(5);
    }

    @Test
    void shipHorizontalOutOfBounds_shouldThrow() {
        List<ShipPlacement> fleet = List.of(
            new ShipPlacement(ShipType.CARRIER, 0, 7, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.BATTLESHIP, 2, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.CRUISER, 4, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.SUBMARINE, 6, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.DESTROYER, 8, 0, Orientation.HORIZONTAL)
        );

        assertThatThrownBy(() -> placementService.validateAndPlaceShips(board, fleet))
            .isInstanceOf(InvalidShipPlacementException.class)
            .satisfies(ex -> assertThat(((InvalidShipPlacementException) ex).getErrorCode()).isEqualTo("SHIP_OUT_OF_BOUNDS"));
    }

    @Test
    void shipVerticalOutOfBounds_shouldThrow() {
        List<ShipPlacement> fleet = List.of(
            new ShipPlacement(ShipType.CARRIER, 8, 0, Orientation.VERTICAL),
            new ShipPlacement(ShipType.BATTLESHIP, 2, 5, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.CRUISER, 4, 5, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.SUBMARINE, 6, 5, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.DESTROYER, 0, 8, Orientation.HORIZONTAL)
        );

        assertThatThrownBy(() -> placementService.validateAndPlaceShips(board, fleet))
            .isInstanceOf(InvalidShipPlacementException.class)
            .satisfies(ex -> assertThat(((InvalidShipPlacementException) ex).getErrorCode()).isEqualTo("SHIP_OUT_OF_BOUNDS"));
    }

    @Test
    void overlappingShips_shouldThrow() {
        List<ShipPlacement> fleet = List.of(
            new ShipPlacement(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.BATTLESHIP, 0, 2, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.CRUISER, 4, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.SUBMARINE, 6, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.DESTROYER, 8, 0, Orientation.HORIZONTAL)
        );

        assertThatThrownBy(() -> placementService.validateAndPlaceShips(board, fleet))
            .isInstanceOf(InvalidShipPlacementException.class)
            .satisfies(ex -> assertThat(((InvalidShipPlacementException) ex).getErrorCode()).isEqualTo("SHIPS_OVERLAP"));
    }

    @Test
    void duplicateShipType_shouldThrow() {
        List<ShipPlacement> fleet = List.of(
            new ShipPlacement(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.CARRIER, 2, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.CRUISER, 4, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.SUBMARINE, 6, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.DESTROYER, 8, 0, Orientation.HORIZONTAL)
        );

        assertThatThrownBy(() -> placementService.validateAndPlaceShips(board, fleet))
            .isInstanceOf(InvalidShipPlacementException.class)
            .satisfies(ex -> assertThat(((InvalidShipPlacementException) ex).getErrorCode()).isEqualTo("INVALID_FLEET_COMPOSITION"));
    }

    @Test
    void incompleteFleet_shouldThrow() {
        List<ShipPlacement> fleet = List.of(
            new ShipPlacement(ShipType.CARRIER, 0, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.BATTLESHIP, 2, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.CRUISER, 4, 0, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.SUBMARINE, 6, 0, Orientation.HORIZONTAL)
        );

        assertThatThrownBy(() -> placementService.validateAndPlaceShips(board, fleet))
            .isInstanceOf(InvalidShipPlacementException.class)
            .satisfies(ex -> assertThat(((InvalidShipPlacementException) ex).getErrorCode()).isEqualTo("INVALID_FLEET_COMPOSITION"));
    }

    @Test
    void shipAtEdge_shouldSucceed() {
        List<ShipPlacement> fleet = List.of(
            new ShipPlacement(ShipType.CARRIER, 0, 5, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.BATTLESHIP, 2, 6, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.CRUISER, 4, 7, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.SUBMARINE, 6, 7, Orientation.HORIZONTAL),
            new ShipPlacement(ShipType.DESTROYER, 8, 8, Orientation.HORIZONTAL)
        );

        placementService.validateAndPlaceShips(board, fleet);
        assertThat(board.isReady()).isTrue();
    }

    @Test
    void validFleet_shouldMarkCellsWithShip() {
        placementService.validateAndPlaceShips(board, validFleet());

        long shipCells = board.getCells().stream().filter(Cell::isHasShip).count();
        // 5+4+3+3+2 = 17 cells
        assertThat(shipCells).isEqualTo(17);
    }
}
