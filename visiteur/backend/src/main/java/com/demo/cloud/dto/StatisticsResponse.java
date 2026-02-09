package com.demo.cloud.dto;

import java.math.BigDecimal;

public record StatisticsResponse(
    long totalSignalements,
    long countNew,
    long countInProgress,
    long countDone,
    BigDecimal avgProgressPercent,
    BigDecimal totalSurfaceM2,
    BigDecimal totalBudget,
    Double avgCompletionDays,
    Double avgStartDelayDays,
    Double avgTreatmentDays
) {}
