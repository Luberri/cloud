package com.demo.cloud.dto;

import java.util.UUID;

public record CreateImageIssueRequest(
    UUID roadIssueId,
    String storagePath,
    String downloadUrl,
    String thumbnailUrl
) {}