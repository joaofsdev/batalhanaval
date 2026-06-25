package com.softexpert.batalhanaval_api.dto.request;

import com.softexpert.batalhanaval_api.domain.Orientation;
import com.softexpert.batalhanaval_api.domain.ShipType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ShipPlacement(
    @NotNull ShipType shipType,
    @Min(0) @Max(9) int originRow,
    @Min(0) @Max(9) int originCol,
    @NotNull Orientation orientation
) {}
