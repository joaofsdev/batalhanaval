package com.softexpert.batalhanaval_api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record FireRequest(
    @Min(0) @Max(9) int row,
    @Min(0) @Max(9) int col
) {}
