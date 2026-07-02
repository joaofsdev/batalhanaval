package com.softexpert.batalhanaval_api.dto.request;

import com.softexpert.batalhanaval_api.domain.AbilityType;
import jakarta.validation.constraints.NotNull;

public record UseAbilityRequest(
    @NotNull AbilityType abilityType,
    Integer row,
    Integer col,
    String axis,
    Integer index
) {}
