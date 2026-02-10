package com.demo.cloud.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RoadIssuePointResponse(
    UUID id,
    String title,
    String description,
    double latitude,
    double longitude,
    BigDecimal surfaceM2,
    BigDecimal budget,
    String statusCode,
    String statusLabel,
    LocalDateTime reportedAt,
    String companyName,
    Integer niveau,
    String niveauLabel
) {}
