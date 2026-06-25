package com.softexpert.batalhanaval_api.dto.response;

import java.util.List;

public record OpponentBoardResponse(
    List<ShotSummary> shotsReceived
) {}
