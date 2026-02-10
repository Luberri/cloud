package com.demo.cloud.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "image_issues")
public class ImageIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "road_issue_id", nullable = false)
    private UUID roadIssueId;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "download_url")
    private String downloadUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Relation (optionnelle)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "road_issue_id", insertable = false, updatable = false)
    private RoadIssue roadIssue;

    // Constructeurs
    public ImageIssue() {}

    public ImageIssue(UUID roadIssueId, String storagePath) {
        this.roadIssueId = roadIssueId;
        this.storagePath = storagePath;
        this.createdAt = LocalDateTime.now();
    }

    // Getters et Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRoadIssueId() { return roadIssueId; }
    public void setRoadIssueId(UUID roadIssueId) { this.roadIssueId = roadIssueId; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public RoadIssue getRoadIssue() { return roadIssue; }
}