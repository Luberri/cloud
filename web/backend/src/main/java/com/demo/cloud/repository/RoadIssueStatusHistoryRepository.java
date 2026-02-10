package com.demo.cloud.repository;

import com.demo.cloud.entity.RoadIssueStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoadIssueStatusHistoryRepository extends JpaRepository<RoadIssueStatusHistory, Integer> {
    
    List<RoadIssueStatusHistory> findByRoadIssueIdOrderByChangedAtDesc(UUID roadIssueId);
    
    List<RoadIssueStatusHistory> findByStatusId(Integer statusId);
    
    List<RoadIssueStatusHistory> findByChangedBy(UUID changedBy);
}