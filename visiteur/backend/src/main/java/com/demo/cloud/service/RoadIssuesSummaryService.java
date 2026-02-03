package com.demo.cloud.service;

import com.demo.cloud.dto.RoadIssuesSummaryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RoadIssuesSummaryService {

    @PersistenceContext
    private EntityManager entityManager;

    public RoadIssuesSummaryResponse getSummary() {
        // Works with or without the SQL VIEW; uses a native query directly.
        Object[] row = (Object[]) entityManager.createNativeQuery("""
            SELECT
                COUNT(*)::bigint AS total_signalements,
                COALESCE(SUM(r.surface_m2), 0) AS total_surface_m2,
                COALESCE(SUM(r.budget), 0) AS total_budget,
                COALESCE(
                    ROUND(
                        100.0 * SUM(CASE WHEN s.code = 'DONE' THEN 1 ELSE 0 END)
                        / NULLIF(COUNT(*), 0),
                        2
                    ),
                    0
                ) AS progress_percent
            FROM road_issues r
            LEFT JOIN road_issue_status s ON s.id = r.status_id
            """).getSingleResult();

        long totalSignalements = ((Number) row[0]).longValue();
        BigDecimal totalSurfaceM2 = toBigDecimal(row[1]);
        BigDecimal totalBudget = toBigDecimal(row[2]);
        BigDecimal progressPercent = toBigDecimal(row[3]);

        return new RoadIssuesSummaryResponse(totalSignalements, totalSurfaceM2, totalBudget, progressPercent);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(value.toString());
    }
}
