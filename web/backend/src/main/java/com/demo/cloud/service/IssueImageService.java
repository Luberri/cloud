package com.demo.cloud.service;

import com.demo.cloud.dto.IssueImageResponse;
import com.demo.cloud.entity.IssueImage;
import com.demo.cloud.repository.IssueImageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class IssueImageService {

    private final IssueImageRepository imageRepository;

    public IssueImageService(IssueImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public List<IssueImageResponse> getImagesByRoadIssue(UUID roadIssueId) {
        return imageRepository.findByRoadIssueIdOrderByCreatedAtDesc(roadIssueId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public long countImagesByRoadIssue(UUID roadIssueId) {
        return imageRepository.countByRoadIssueId(roadIssueId);
    }

    private IssueImageResponse toResponse(IssueImage image) {
        return new IssueImageResponse(
            image.getId(),
            image.getRoadIssueId(),
            image.getDownloadUrl(),
            image.getThumbnailUrl(),
            image.getFileSizeBytes(),
            image.getMimeType(),
            image.getCreatedAt()
        );
    }
}
