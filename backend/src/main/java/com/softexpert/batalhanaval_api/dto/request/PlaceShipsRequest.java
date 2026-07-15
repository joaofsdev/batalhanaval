package com.softexpert.batalhanaval_api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Posicionamento completo da frota (5 navios)")
public record PlaceShipsRequest(
    @NotNull @Size(min = 5, max = 5)
    @Schema(description = "Lista de 5 posicionamentos de navios")
    List<@Valid ShipPlacement> ships
) {}
