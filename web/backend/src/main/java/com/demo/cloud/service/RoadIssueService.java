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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class RoadIssueService {

    @Autowired
    private RoadIssueRepository roadIssueRepository;

    @Autowired
    private SyncLogRepository syncLogRepository;

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private RoadIssueStatusHistoryService historyService;

    /**
     * SYNC BIDIRECTIONNEL: Compare Postgres et Firestore, update si status_id a changé
     */
    @Transactional
    public int syncWithFirebase() {
        int count = 0;

        // 1) PUSH: Issues locaux non synchronisés -> Firestore
        List<RoadIssue> unsyncedIssues = roadIssueRepository.findByIsSyncedFalse();
        for (RoadIssue issue : unsyncedIssues) {
            try {
                String firebaseId = firebaseService.pushRoadIssue(issue);
                if (firebaseId != null && !firebaseId.isEmpty()) {
                    issue.setIsSynced(true);
                    issue.setFirebaseId(firebaseId);
                    roadIssueRepository.save(issue);
                    logSync("PUSH", "road_issue", "SUCCESS",
                            "ID: " + issue.getId() + " -> Firebase ID: " + firebaseId);
                    count++;
                }
            } catch (Exception e) {
                logSync("PUSH", "road_issue", "ERROR",
                        "ID: " + issue.getId() + " - Erreur: " + e.getMessage());
            }
        }

        // 2) CHECK: Issues déjà synchronisés - vérifier si status_id a changé
        count += syncStatusChangesToFirebase();

        return count;
    }

    /**
     * Vérifie tous les issues synchronisés et met à jour Firebase si status_id a changé
     */
    @Transactional
    public int syncStatusChangesToFirebase() {
        int count = 0;

        // Récupérer tous les issues qui ont un firebase_id (déjà synchronisés)
        List<RoadIssue> syncedIssues = roadIssueRepository.findByFirebaseIdIsNotNull();

        // Récupérer les données actuelles de Firestore
        Map<String, Map<String, Object>> firestoreData = new HashMap<>();
        try {
            List<FirebaseService.FirestoreRoadIssue> firebaseDocs = firebaseService.fetchRoadIssues();
            for (FirebaseService.FirestoreRoadIssue doc : firebaseDocs) {
                firestoreData.put(doc.docId(), doc.data());
            }
        } catch (Exception e) {
            logSync("SYNC", "road_issue", "ERROR", "Impossible de récupérer Firestore: " + e.getMessage());
            return 0;
        }

        for (RoadIssue localIssue : syncedIssues) {
            try {
                String firebaseId = localIssue.getFirebaseId();
                Map<String, Object> remoteData = firestoreData.get(firebaseId);

                if (remoteData == null) {
                    // Le doc n'existe plus sur Firestore, on le recrée
                    String newFirebaseId = firebaseService.pushRoadIssue(localIssue);
                    if (newFirebaseId != null) {
                        localIssue.setFirebaseId(newFirebaseId);
                        roadIssueRepository.save(localIssue);
                        logSync("PUSH", "road_issue", "SUCCESS",
                                "Recréé sur Firebase: " + localIssue.getId());
                        count++;
                    }
                    continue;
                }

                // Comparer status_id local vs remote
                Integer localStatusId = localIssue.getStatusId();
                Integer remoteStatusId = asInt(remoteData.get("statusId"));

                boolean statusChanged = !Objects.equals(localStatusId, remoteStatusId);

                // Comparer aussi updatedAt pour détecter d'autres changements
                LocalDateTime localUpdatedAt = localIssue.getUpdatedAt();
                LocalDateTime remoteUpdatedAt = asLocalDateTime(remoteData.get("updatedAt"));

                boolean needsUpdate =
                        statusChanged ||
                                (localUpdatedAt != null && remoteUpdatedAt != null &&
                                        localUpdatedAt.isAfter(remoteUpdatedAt));

                if (needsUpdate) {
                    // Mettre à jour Firestore avec les données locales
                    firebaseService.pushRoadIssue(localIssue);

                    logSync("PUSH", "road_issue", "SUCCESS",
                            "Status changé: " + localIssue.getId() +
                                    " (local=" + localStatusId + ", remote=" + remoteStatusId + ")");
                    count++;
                }

            } catch (Exception e) {
                logSync("SYNC", "road_issue", "ERROR",
                        "Issue " + localIssue.getId() + " - " + e.getMessage());
            }
        }

        return count;
    }

    /**
     * PUSH: un seul RoadIssue -> Firestore (update/merge du doc existant)
     */
    @Transactional
    public void pushSingleIssue(RoadIssue issue) {
        try {
            String firebaseId = firebaseService.pushRoadIssue(issue);

            if (firebaseId != null && !firebaseId.isBlank()) {
                issue.setIsSynced(true);
                issue.setFirebaseId(firebaseId);
                roadIssueRepository.save(issue);

                SyncLog log = new SyncLog();
                log.setSyncType("PUSH");
                log.setEntity("road_issue");
                log.setStatus("SUCCESS");
                log.setMessage("UPDATE ID: " + issue.getId() + " -> Firebase ID: " + firebaseId);
                syncLogRepository.save(log);
            }
        } catch (Exception e) {
            SyncLog log = new SyncLog();
            log.setSyncType("PUSH");
            log.setEntity("road_issue");
            log.setStatus("ERROR");
            log.setMessage("UPDATE ID: " + issue.getId() + " - Erreur: " + e.getMessage());
            syncLogRepository.save(log);
        }
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Double asDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal asBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static UUID asUuid(Object o) {
        if (o == null) return null;
        try {
            return UUID.fromString(o.toString());
        } catch (Exception e) {
            return null;
        }
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
        try {
            return LocalDateTime.parse(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private void logSync(String syncType, String entity, String status, String message) {
        SyncLog log = new SyncLog();
        log.setSyncType(syncType);
        log.setEntity(entity);
        log.setStatus(status);
        log.setMessage(message);
        syncLogRepository.save(log);
    }
}
