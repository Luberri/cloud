package com.demo.cloud.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ImageIssueResponse(
    UUID id,
    UUID roadIssueId,
    String storagePath,
    String downloadUrl,
    String thumbnailUrl,
    LocalDateTime createdAt
) {}