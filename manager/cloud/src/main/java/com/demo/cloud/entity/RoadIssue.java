package com.demo.cloud.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "road_issues")
public class RoadIssue {

    @Id
    private UUID id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "surface_m2")
    private BigDecimal surfaceM2;

    private BigDecimal budget;

    @Column(name = "status_id")
    private Integer statusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", insertable = false, updatable = false)
    private RoadIssueStatus status;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "reported_by")
    private UUID reportedBy;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt;

    @Column(name = "is_synced")
    private Boolean isSynced;

    @Column(name = "firebase_id")
    private String firebaseId;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getSurfaceM2() {
        return surfaceM2;
    }

    public void setSurfaceM2(BigDecimal surfaceM2) {
        this.surfaceM2 = surfaceM2;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    public RoadIssueStatus getStatus() {
        return status;
    }

    public void setStatus(RoadIssueStatus status) {
        this.status = status;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public UUID getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(UUID reportedBy) {
        this.reportedBy = reportedBy;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public Boolean getIsSynced() {
        return isSynced;
    }

    public void setIsSynced(Boolean isSynced) {
        this.isSynced = isSynced;
    }

    public String getFirebaseId() {
        return firebaseId;
    }

    public void setFirebaseId(String firebaseId) {
        this.firebaseId = firebaseId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
