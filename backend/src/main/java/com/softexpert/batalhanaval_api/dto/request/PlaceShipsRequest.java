package com.softexpert.batalhanaval_api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaceShipsRequest(
    @NotNull @Size(min = 5, max = 5) List<@Valid ShipPlacement> ships
) {}
