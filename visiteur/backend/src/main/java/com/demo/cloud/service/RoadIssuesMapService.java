package com.demo.cloud.service;

import com.demo.cloud.dto.RoadIssuePointResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoadIssuesMapService {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<RoadIssuePointResponse> getAllRoadIssuePoints() {
        List<Object[]> results = entityManager.createNativeQuery("""
            SELECT
                r.id,
                r.title,
                r.description,
                ST_Y(r.location::geometry) AS latitude,
                ST_X(r.location::geometry) AS longitude,
                r.surface_m2,
                COALESCE(
                    (SELECT prix FROM prix_forfaitaire ORDER BY updated_at DESC LIMIT 1), 
                    50000
                ) * COALESCE(r.niveau, 1) * COALESCE(r.surface_m2, 0) AS budget,
                s.code AS status_code,
                s.label AS status_label,
                r.reported_at,
                c.name AS company_name,
                r.niveau,
                CASE 
                    WHEN r.niveau BETWEEN 1 AND 3 THEN 'Faible'
                    WHEN r.niveau BETWEEN 4 AND 6 THEN 'Moyen'
                    WHEN r.niveau BETWEEN 7 AND 10 THEN 'Critique'
                    ELSE 'Non défini'
                END AS niveau_label
            FROM road_issues r
            LEFT JOIN road_issue_status s ON s.id = r.status_id
            LEFT JOIN companies c ON c.id = r.company_id
            ORDER BY r.reported_at DESC
            """).getResultList();

        return results.stream()
            .map(row -> new RoadIssuePointResponse(
                (UUID) row[0],
                (String) row[1],
                (String) row[2],
                toDouble(row[3]),
                toDouble(row[4]),
                toBigDecimal(row[5]),
                toBigDecimal(row[6]),
                (String) row[7],
                (String) row[8],
                toLocalDateTime(row[9]),
                (String) row[10],
                toInteger(row[11]),
                (String) row[12]
            ))
            .collect(Collectors.toList());
    }

    private static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(value.toString());
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(value.toString());
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof Timestamp ts) return ts.toLocalDateTime();
        if (value instanceof LocalDateTime ldt) return ldt;
        return null;
    }

    private static Integer toInteger(Object value) {
        if (value == null) return 1;
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        return Integer.parseInt(value.toString());
    }
}
