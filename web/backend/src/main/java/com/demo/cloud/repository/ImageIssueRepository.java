package com.demo.cloud.repository;

import com.demo.cloud.entity.ImageIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageIssueRepository extends JpaRepository<ImageIssue, UUID> {
    
    List<ImageIssue> findByRoadIssueId(UUID roadIssueId);
    
    Optional<ImageIssue> findByRoadIssueIdAndStoragePath(UUID roadIssueId, String storagePath);
    
    void deleteByRoadIssueId(UUID roadIssueId);
}