package com.softexpert.batalhanaval_api.dto.response;

public record CellResponse(
    int row,
    int col,
    boolean hasShip,
    boolean hit
) {}
