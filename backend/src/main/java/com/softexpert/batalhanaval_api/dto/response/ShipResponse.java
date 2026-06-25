package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.Orientation;
import com.softexpert.batalhanaval_api.domain.ShipType;

public record ShipResponse(
    ShipType shipType,
    int originRow,
    int originCol,
    Orientation orientation,
    int hits,
    boolean sunk
) {}
