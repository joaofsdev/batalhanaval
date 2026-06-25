package com.softexpert.batalhanaval_api.domain;

import lombok.Getter;

@Getter
public enum ShipType {

    CARRIER(5, "Porta-aviões"),
    BATTLESHIP(4, "Encouraçado"),
    CRUISER(3, "Cruzador"),
    SUBMARINE(3, "Submarino"),
    DESTROYER(2, "Contratorpedeiro");

    private final int size;
    private final String displayName;

    ShipType(int size, String displayName) {
        this.size = size;
        this.displayName = displayName;
    }
}
