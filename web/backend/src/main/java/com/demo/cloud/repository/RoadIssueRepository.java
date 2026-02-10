package com.demo.cloud.repository;

import com.demo.cloud.entity.RoadIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoadIssueRepository extends JpaRepository<RoadIssue, UUID> {
    List<RoadIssue> findByIsSyncedFalse();
    Optional<RoadIssue> findByFirebaseId(String firebaseId);
    List<RoadIssue> findByFirebaseIdIsNotNull();
}
