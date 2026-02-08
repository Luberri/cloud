package com.demo.cloud.service;

import com.demo.cloud.dto.CreateImageIssueRequest;
import com.demo.cloud.dto.ImageIssueResponse;
import com.demo.cloud.entity.ImageIssue;
import com.demo.cloud.repository.ImageIssueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ImageIssueService {

    private final ImageIssueRepository imageIssueRepository;

    public ImageIssueService(ImageIssueRepository imageIssueRepository) {
        this.imageIssueRepository = imageIssueRepository;
    }

    @Transactional
    public ImageIssueResponse addImage(CreateImageIssueRequest request) {
        // Vérifier si l'image existe déjà (éviter doublons)
        var existing = imageIssueRepository.findByRoadIssueIdAndStoragePath(
            request.roadIssueId(), 
            request.storagePath()
        );
        
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        ImageIssue image = new ImageIssue(request.roadIssueId(), request.storagePath());
        image.setDownloadUrl(request.downloadUrl());
        image.setThumbnailUrl(request.thumbnailUrl());
        
        ImageIssue saved = imageIssueRepository.save(image);
        return toResponse(saved);
    }

    public List<ImageIssueResponse> getImagesByRoadIssue(UUID roadIssueId) {
        return imageIssueRepository.findByRoadIssueId(roadIssueId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteImage(UUID imageId) {
        imageIssueRepository.deleteById(imageId);
    }

    @Transactional
    public void deleteAllImagesForRoadIssue(UUID roadIssueId) {
        imageIssueRepository.deleteByRoadIssueId(roadIssueId);
    }

    private ImageIssueResponse toResponse(ImageIssue image) {
        return new ImageIssueResponse(
            image.getId(),
            image.getRoadIssueId(),
            image.getStoragePath(),
            image.getDownloadUrl(),
            image.getThumbnailUrl(),
            image.getCreatedAt()
        );
    }
}
