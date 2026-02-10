package com.demo.cloud.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "road_issue_status_history")
public class RoadIssueStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "road_issue_id", nullable = false)
    private UUID roadIssueId;

    @Column(name = "status_id", nullable = false)
    private Integer statusId;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by")
    private UUID changedBy;

    public RoadIssueStatusHistory() {
        this.changedAt = LocalDateTime.now();
    }

    // Added constructor to match service usage
    public RoadIssueStatusHistory(UUID roadIssueId, Integer statusId, UUID changedBy) {
        this.roadIssueId = roadIssueId;
        this.statusId = statusId;
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
    }

    // Getters et Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public UUID getRoadIssueId() { return roadIssueId; }
    public void setRoadIssueId(UUID roadIssueId) { this.roadIssueId = roadIssueId; }

    public Integer getStatusId() { return statusId; }
    public void setStatusId(Integer statusId) { this.statusId = statusId; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }

    public UUID getChangedBy() { return changedBy; }
    public void setChangedBy(UUID changedBy) { this.changedBy = changedBy; }
}
