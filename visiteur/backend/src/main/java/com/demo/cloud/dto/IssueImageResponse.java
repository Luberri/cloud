package com.demo.cloud.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record IssueImageResponse(
    UUID id,
    UUID roadIssueId,
    String downloadUrl,
    String thumbnailUrl,
    Long fileSizeBytes,
    String mimeType,
    LocalDateTime createdAt
) {}
