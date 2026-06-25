package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.*;
import com.softexpert.batalhanaval_api.dto.request.ShipPlacement;
import com.softexpert.batalhanaval_api.exception.InvalidShipPlacementException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PlacementService {

    public void validateAndPlaceShips(Board board, List<ShipPlacement> placements) {
        validateFleetComposition(placements);

        Set<String> occupiedCells = new HashSet<>();
        List<Ship> ships = new ArrayList<>();

        for (ShipPlacement placement : placements) {
            List<int[]> coords = calculateOccupiedCoordinates(placement);
            validateWithinBounds(coords);
            validateNoOverlap(coords, occupiedCells);

            Ship ship = new Ship();
            ship.setBoard(board);
            ship.setShipType(placement.shipType());
            ship.setOriginRow(placement.originRow());
            ship.setOriginCol(placement.originCol());
            ship.setOrientation(placement.orientation());
            ship.setHits(0);
            ships.add(ship);

            for (int[] coord : coords) {
                occupiedCells.add(coord[0] + "," + coord[1]);
            }
        }

        board.getShips().addAll(ships);

        for (Cell cell : board.getCells()) {
            String key = cell.getRow() + "," + cell.getCol();
            if (occupiedCells.contains(key)) {
                cell.setHasShip(true);
                Ship ownerShip = findShipForCoordinate(ships, cell.getRow(), cell.getCol());
                cell.setShip(ownerShip);
            }
        }

        board.setReady(true);
    }

    private void validateFleetComposition(List<ShipPlacement> placements) {
        Set<ShipType> types = EnumSet.noneOf(ShipType.class);
        for (ShipPlacement p : placements) {
            if (!types.add(p.shipType())) {
                throw new InvalidShipPlacementException("Duplicate ship type: " + p.shipType(), "INVALID_FLEET_COMPOSITION");
            }
        }
        if (types.size() != 5 || !types.equals(EnumSet.allOf(ShipType.class))) {
            throw new InvalidShipPlacementException("Fleet must contain exactly one of each ship type", "INVALID_FLEET_COMPOSITION");
        }
    }

    private List<int[]> calculateOccupiedCoordinates(ShipPlacement placement) {
        List<int[]> coords = new ArrayList<>();
        int size = placement.shipType().getSize();
        for (int i = 0; i < size; i++) {
            int row = placement.originRow() + (placement.orientation() == Orientation.VERTICAL ? i : 0);
            int col = placement.originCol() + (placement.orientation() == Orientation.HORIZONTAL ? i : 0);
            coords.add(new int[]{row, col});
        }
        return coords;
    }

    private void validateWithinBounds(List<int[]> coords) {
        for (int[] coord : coords) {
            if (coord[0] < 0 || coord[0] > 9 || coord[1] < 0 || coord[1] > 9) {
                throw new InvalidShipPlacementException("Ship extends out of bounds", "SHIP_OUT_OF_BOUNDS");
            }
        }
    }

    private void validateNoOverlap(List<int[]> coords, Set<String> occupied) {
        for (int[] coord : coords) {
            String key = coord[0] + "," + coord[1];
            if (occupied.contains(key)) {
                throw new InvalidShipPlacementException("Ships overlap at (" + coord[0] + "," + coord[1] + ")", "SHIPS_OVERLAP");
            }
        }
    }

    private Ship findShipForCoordinate(List<Ship> ships, int row, int col) {
        for (Ship ship : ships) {
            int size = ship.getShipType().getSize();
            for (int i = 0; i < size; i++) {
                int sRow = ship.getOriginRow() + (ship.getOrientation() == Orientation.VERTICAL ? i : 0);
                int sCol = ship.getOriginCol() + (ship.getOrientation() == Orientation.HORIZONTAL ? i : 0);
                if (sRow == row && sCol == col) {
                    return ship;
                }
            }
        }
        return null;
    }
}
