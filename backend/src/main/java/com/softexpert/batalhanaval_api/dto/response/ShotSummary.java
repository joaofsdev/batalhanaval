package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.Orientation;
import com.softexpert.batalhanaval_api.domain.ShipType;
import com.softexpert.batalhanaval_api.domain.ShotResult;

public record ShotSummary(
    int row,
    int col,
    ShotResult result,
    ShipType sunkShipType,
    Integer sunkShipOriginRow,
    Integer sunkShipOriginCol,
    Orientation sunkShipOrientation
) {}
