package com.softexpert.batalhanaval_api.dto.response;

import com.softexpert.batalhanaval_api.domain.StormEventType;

public record StormEventNotification(
    StormEventType eventType,
    String affectedAxis,
    String message,
    Boolean shipMoved
) {}
