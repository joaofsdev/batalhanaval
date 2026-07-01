package com.softexpert.batalhanaval_api.dto.response;

import java.util.List;

public record RankingResponse(
    List<RankingEntry> ranking,
    RankingEntry myPosition,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
