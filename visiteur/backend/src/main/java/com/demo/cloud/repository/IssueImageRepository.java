package com.demo.cloud.repository;

import com.demo.cloud.entity.IssueImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueImageRepository extends JpaRepository<IssueImage, UUID> {
    List<IssueImage> findByRoadIssueIdOrderByCreatedAtDesc(UUID roadIssueId);
    long countByRoadIssueId(UUID roadIssueId);
    List<IssueImage> findByRoadIssueId(UUID roadIssueId);
    boolean existsByRoadIssueIdAndStoragePath(UUID roadIssueId, String storagePath);
    void deleteByRoadIssueId(UUID roadIssueId);

}
