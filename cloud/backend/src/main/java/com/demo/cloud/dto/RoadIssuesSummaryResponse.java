package com.demo.cloud.dto;

import java.math.BigDecimal;

public record RoadIssuesSummaryResponse(
    long totalSignalements,
    BigDecimal totalSurfaceM2,
    BigDecimal totalBudget,
    BigDecimal progressPercent
) {}
