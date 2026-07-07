package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.Orientation;
import com.softexpert.batalhanaval_api.domain.ShipType;
import com.softexpert.batalhanaval_api.domain.ShotResult;

import java.util.UUID;

public record ShotResultResponse(
    UUID gameId,
    int row,
    int col,
    ShotResult result,
    ShipType sunkShipType,
    Integer sunkShipOriginRow,
    Integer sunkShipOriginCol,
    Orientation sunkShipOrientation
) {}
