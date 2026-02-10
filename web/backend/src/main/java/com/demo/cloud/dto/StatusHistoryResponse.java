package com.demo.cloud.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record StatusHistoryResponse(
    Integer id,
    UUID roadIssueId,
    Integer statusId,
    String statusCode,
    String statusLabel,
    LocalDateTime changedAt,
    UUID changedBy,
    String changedByName
) {}