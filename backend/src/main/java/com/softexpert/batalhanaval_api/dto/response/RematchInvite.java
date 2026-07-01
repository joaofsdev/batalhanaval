package com.softexpert.batalhanaval_api.dto.response;

import java.util.UUID;

public record RematchInvite(
    UUID gameId,
    String opponentUsername
) {}
