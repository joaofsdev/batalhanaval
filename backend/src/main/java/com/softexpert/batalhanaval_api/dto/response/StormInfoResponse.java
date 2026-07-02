package com.softexpert.batalhanaval_api.dto.response;

public record StormInfoResponse(
    int nextStormTurn,
    int currentTurn,
    int turnsUntilStorm
) {}
