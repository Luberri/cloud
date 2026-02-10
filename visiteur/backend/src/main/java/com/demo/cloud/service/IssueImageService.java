package com.demo.cloud.service;

import com.demo.cloud.dto.ImageUploadResponse;
import com.demo.cloud.entity.IssueImage;
import com.demo.cloud.repository.IssueImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class IssueImageService {

    @Autowired
    private IssueImageRepository imageRepository;

    public List<IssueImage> getImagesByIssueId(UUID issueId) {
        return imageRepository.findByRoadIssueId(issueId);
    }

    public long countImagesByIssueId(UUID issueId) {
        return imageRepository.countByRoadIssueId(issueId);
    }

    public IssueImage getImageById(UUID id) {
        return imageRepository.findById(id).orElse(null);
    }

    public ImageUploadResponse uploadImage(MultipartFile file, UUID issueId, String storagePath) throws IOException {
        IssueImage image = new IssueImage();
        image.setRoadIssueId(issueId);
        image.setStoragePath(storagePath);
        image.setDownloadUrl("/photos/" + storagePath);
        image.setFileSizeBytes(file.getSize());
        image.setMimeType(file.getContentType());
        image.setCreatedAt(LocalDateTime.now());

        IssueImage saved = imageRepository.save(image);

        ImageUploadResponse response = new ImageUploadResponse();
        response.setId(saved.getId());
        response.setImagePath(saved.getStoragePath());
        response.setDownloadUrl(saved.getDownloadUrl());
        response.setUploadedAt(saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : null);

        return response;
    }

    public void deleteImage(UUID id) {
        imageRepository.deleteById(id);
    }

    public void deleteImagesByIssueId(UUID issueId) {
        imageRepository.deleteByRoadIssueId(issueId);
    }
}
