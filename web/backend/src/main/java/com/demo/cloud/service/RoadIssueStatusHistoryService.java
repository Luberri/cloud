package com.demo.cloud.service;

import com.demo.cloud.dto.StatusHistoryResponse;
import com.demo.cloud.entity.RoadIssueStatusHistory;
import com.demo.cloud.repository.RoadIssueStatusHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoadIssueStatusHistoryService {

    private final RoadIssueStatusHistoryRepository historyRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public RoadIssueStatusHistoryService(RoadIssueStatusHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Transactional
    public RoadIssueStatusHistory addStatusChange(UUID roadIssueId, Integer statusId, UUID changedBy) {
        RoadIssueStatusHistory history = new RoadIssueStatusHistory(roadIssueId, statusId, changedBy);
        return historyRepository.save(history);
    }

    public List<StatusHistoryResponse> getHistoryByRoadIssue(UUID roadIssueId) {
        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery("""
            SELECT 
                h.id,
                h.road_issue_id,
                h.status_id,
                s.code AS status_code,
                s.label AS status_label,
                h.changed_at,
                h.changed_by,
                u.full_name AS changed_by_name
            FROM road_issue_status_history h
            LEFT JOIN road_issue_status s ON s.id = h.status_id
            LEFT JOIN users u ON u.id = h.changed_by
            WHERE h.road_issue_id = ?1
            ORDER BY h.changed_at DESC
            """)
            .setParameter(1, roadIssueId)
            .getResultList();

        return results.stream()
            .map(row -> new StatusHistoryResponse(
                (Integer) row[0],
                (UUID) row[1],
                (Integer) row[2],
                (String) row[3],
                (String) row[4],
                row[5] != null ? ((java.sql.Timestamp) row[5]).toLocalDateTime() : null,
                (UUID) row[6],
                (String) row[7]
            ))
            .collect(Collectors.toList());
    }

    public List<RoadIssueStatusHistory> getAllHistory() {
        return historyRepository.findAll();
    }
}