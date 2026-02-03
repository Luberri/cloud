package com.demo.cloud.service;

import com.demo.cloud.entity.RoadIssue;
import com.demo.cloud.entity.SyncLog;
import com.demo.cloud.repository.RoadIssueRepository;
import com.demo.cloud.repository.SyncLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RoadIssueService {

    @Autowired
    private RoadIssueRepository roadIssueRepository;

    @Autowired
    private SyncLogRepository syncLogRepository;

    @Autowired
    private FirebaseService firebaseService;

    /**
     * PUSH: DB locale -> Firestore (déjà existant)
     */
    @Transactional
    public int syncWithFirebase() {
        List<RoadIssue> issues = roadIssueRepository.findByIsSyncedFalse();
        int count = 0;

        for (RoadIssue issue : issues) {
            try {
                String firebaseId = firebaseService.pushRoadIssue(issue);

                if (firebaseId != null && !firebaseId.isEmpty()) {
                    issue.setIsSynced(true);
                    issue.setFirebaseId(firebaseId);
                    roadIssueRepository.save(issue);

                    SyncLog log = new SyncLog();
                    log.setSyncType("PUSH");
                    log.setEntity("road_issue");
                    log.setStatus("SUCCESS");
                    log.setMessage("ID: " + issue.getId() + " -> Firebase ID: " + firebaseId);
                    syncLogRepository.save(log);

                    count++;
                } else {
                    SyncLog log = new SyncLog();
                    log.setSyncType("PUSH");
                    log.setEntity("road_issue");
                    log.setStatus("FAIL");
                    log.setMessage("ID: " + issue.getId() + " - Firebase ID vide");
                    syncLogRepository.save(log);
                }
            } catch (Exception e) {
                SyncLog log = new SyncLog();
                log.setSyncType("PUSH");
                log.setEntity("road_issue");
                log.setStatus("ERROR");
                log.setMessage("ID: " + issue.getId() + " - Erreur: " + e.getMessage());
                syncLogRepository.save(log);
            }
        }

        return count;
    }

    /**
     * PULL: Firestore -> DB locale (upsert via firebase_id)
     */
    @Transactional
    public int pullFromFirebase() {
        int count = 0;

        List<FirebaseService.FirestoreRoadIssue> docs = firebaseService.fetchRoadIssues();
        for (FirebaseService.FirestoreRoadIssue doc : docs) {
            try {
                String firebaseId = doc.docId();
                Map<String, Object> data = doc.data();

                RoadIssue issue = roadIssueRepository.findByFirebaseId(firebaseId).orElseGet(RoadIssue::new);

                // si le doc contient l'uuid local d'origine, on le garde (sinon DB génère)
                UUID id = asUuid(data.get("id"));
                if (issue.getId() == null && id != null) {
                    issue.setId(id);
                }

                issue.setFirebaseId(firebaseId);
                issue.setTitle(asString(data.get("title")));
                issue.setDescription(asString(data.get("description")));

                Double lat = asDouble(data.get("latitude"));
                Double lon = asDouble(data.get("longitude"));
                if (lat != null && lon != null) {
                    issue.setLocationFromCoords(lat, lon);
                }

                issue.setSurfaceM2(asBigDecimal(data.get("surfaceM2")));
                issue.setBudget(asBigDecimal(data.get("budget")));
                issue.setStatusId(asInt(data.get("statusId")));
                issue.setCompanyId(asInt(data.get("companyId")));

                issue.setReportedBy(asUuid(data.get("reportedBy")));
                issue.setReportedAt(asLocalDateTime(data.get("reportedAt")));
                issue.setUpdatedAt(asLocalDateTime(data.get("updatedAt")));

                issue.setIsSynced(true);

                roadIssueRepository.save(issue);

                SyncLog log = new SyncLog();
                log.setSyncType("PULL");
                log.setEntity("road_issue");
                log.setStatus("SUCCESS");
                log.setMessage("firebaseId=" + firebaseId + " -> localId=" + issue.getId());
                syncLogRepository.save(log);

                count++;
            } catch (Exception e) {
                SyncLog log = new SyncLog();
                log.setSyncType("PULL");
                log.setEntity("road_issue");
                log.setStatus("ERROR");
                log.setMessage("firebaseId=" + doc.docId() + " - Erreur: " + e.getMessage());
                syncLogRepository.save(log);
            }
        }

        return count;
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Double asDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return null; }
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return null; }
    }

    private static BigDecimal asBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(o.toString()); } catch (Exception e) { return null; }
    }

    private static UUID asUuid(Object o) {
        if (o == null) return null;
        try { return UUID.fromString(o.toString()); } catch (Exception e) { return null; }
    }

    private static LocalDateTime asLocalDateTime(Object o) {
        if (o == null) return null;

        // Firestore Timestamp
        if (o instanceof com.google.cloud.Timestamp ts) {
            return LocalDateTime.ofInstant(ts.toDate().toInstant(), ZoneOffset.UTC);
        }

        // java.util.Date
        if (o instanceof Date d) {
            return LocalDateTime.ofInstant(d.toInstant(), ZoneOffset.UTC);
        }

        // String ISO-8601 (si jamais)
        try { return LocalDateTime.parse(o.toString()); } catch (Exception e) { return null; }
    }
}
