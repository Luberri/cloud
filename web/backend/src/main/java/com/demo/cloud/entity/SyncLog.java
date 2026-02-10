package com.demo.cloud.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sync_logs")
public class SyncLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "sync_type", length = 50)
    private String syncType; // PUSH / PULL
    
    @Column(name = "entity", length = 50)
    private String entity; // road_issue, user
    
    @Column(name = "synced_at")
    private LocalDateTime syncedAt;
    
    @Column(name = "status", length = 30)
    private String status; // SUCCESS / FAIL / ERROR
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    public SyncLog() {
        this.syncedAt = LocalDateTime.now();
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    
    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
    
    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}