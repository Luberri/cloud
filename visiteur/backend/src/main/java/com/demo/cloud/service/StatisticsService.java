package com.demo.cloud.service;

import com.demo.cloud.dto.StatisticsResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class StatisticsService {

    @PersistenceContext
    private EntityManager entityManager;

    public StatisticsResponse getStatistics() {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
            SELECT
                COUNT(*)::bigint AS total_signalements,

                SUM(CASE WHEN s.code = 'NEW' THEN 1 ELSE 0 END)::bigint AS count_new,
                SUM(CASE WHEN s.code = 'IN_PROGRESS' THEN 1 ELSE 0 END)::bigint AS count_in_progress,
                SUM(CASE WHEN s.code = 'DONE' THEN 1 ELSE 0 END)::bigint AS count_done,

                COALESCE(ROUND(AVG(
                    CASE
                        WHEN s.code = 'DONE' THEN 100
                        WHEN s.code = 'IN_PROGRESS' THEN 50
                        ELSE 0
                    END
                ), 2), 0) AS avg_progress_percent,

                COALESCE(SUM(r.surface_m2), 0) AS total_surface_m2,
                COALESCE(SUM(r.budget), 0) AS total_budget,

                -- Délai moyen total : de la première entrée (NEW) à DONE (en jours)
                ROUND(AVG(
                    CASE WHEN done_h.changed_at IS NOT NULL AND new_h.changed_at IS NOT NULL
                    THEN EXTRACT(EPOCH FROM (done_h.changed_at - new_h.changed_at)) / 86400.0
                    END
                )::numeric, 2) AS avg_completion_days,

                -- Délai moyen prise en charge : de NEW à IN_PROGRESS (en jours)
                ROUND(AVG(
                    CASE WHEN ip_h.changed_at IS NOT NULL AND new_h.changed_at IS NOT NULL
                    THEN EXTRACT(EPOCH FROM (ip_h.changed_at - new_h.changed_at)) / 86400.0
                    END
                )::numeric, 2) AS avg_start_delay_days,

                -- Délai moyen traitement : de IN_PROGRESS à DONE (en jours)
                ROUND(AVG(
                    CASE WHEN done_h.changed_at IS NOT NULL AND ip_h.changed_at IS NOT NULL
                    THEN EXTRACT(EPOCH FROM (done_h.changed_at - ip_h.changed_at)) / 86400.0
                    END
                )::numeric, 2) AS avg_treatment_days

            FROM road_issues r
            LEFT JOIN road_issue_status s ON s.id = r.status_id

            -- Première entrée dans l'historique avec statut NEW
            LEFT JOIN LATERAL (
                SELECT h.changed_at
                FROM road_issue_status_history h
                JOIN road_issue_status st ON st.id = h.status_id
                WHERE h.road_issue_id = r.id AND st.code = 'NEW'
                ORDER BY h.changed_at ASC
                LIMIT 1
            ) new_h ON true

            -- Première entrée dans l'historique avec statut IN_PROGRESS
            LEFT JOIN LATERAL (
                SELECT h.changed_at
                FROM road_issue_status_history h
                JOIN road_issue_status st ON st.id = h.status_id
                WHERE h.road_issue_id = r.id AND st.code = 'IN_PROGRESS'
                ORDER BY h.changed_at ASC
                LIMIT 1
            ) ip_h ON true

            -- Première entrée dans l'historique avec statut DONE
            LEFT JOIN LATERAL (
                SELECT h.changed_at
                FROM road_issue_status_history h
                JOIN road_issue_status st ON st.id = h.status_id
                WHERE h.road_issue_id = r.id AND st.code = 'DONE'
                ORDER BY h.changed_at ASC
                LIMIT 1
            ) done_h ON true
            """).getSingleResult();

        return new StatisticsResponse(
            ((Number) row[0]).longValue(),
            ((Number) row[1]).longValue(),
            ((Number) row[2]).longValue(),
            ((Number) row[3]).longValue(),
            toBigDecimal(row[4]),
            toBigDecimal(row[5]),
            toBigDecimal(row[6]),
            toDouble(row[7]),
            toDouble(row[8]),
            toDouble(row[9])
        );
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(value.toString());
    }

    private static Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(value.toString()); } catch (Exception e) { return null; }
    }
}
