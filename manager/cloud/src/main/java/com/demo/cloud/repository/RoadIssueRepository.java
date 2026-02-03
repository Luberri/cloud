package com.demo.cloud.repository;

import com.demo.cloud.entity.RoadIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoadIssueRepository extends JpaRepository<RoadIssue, UUID> {
}
