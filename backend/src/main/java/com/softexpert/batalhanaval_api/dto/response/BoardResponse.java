package com.softexpert.batalhanaval_api.dto.response;

import java.util.List;

public record BoardResponse(
    boolean ready,
    List<ShipResponse> ships,
    List<CellResponse> cells
) {}
