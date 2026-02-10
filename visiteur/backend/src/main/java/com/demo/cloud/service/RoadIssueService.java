package com.demo.cloud.service;

import com.demo.cloud.entity.RoadIssue;
import com.demo.cloud.entity.SyncLog;
import com.demo.cloud.repository.RoadIssueRepository;
import com.demo.cloud.repository.SyncLogRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

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

    private final GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * SYNC BIDIRECTIONNEL: Compare Postgres et Firestore, update si status_id a changé
     * ET crée les nouveaux signalements depuis Firebase
     */
    @Transactional
    public int syncWithFirebase() {
        int totalSynced = 0;
        
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> future = firestore.collection("road_issues").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            System.out.println("\n=== SYNCHRONISATION ROAD_ISSUES ===");
            System.out.println("📥 " + documents.size() + " documents Firebase trouvés\n");

            for (QueryDocumentSnapshot doc : documents) {
                String firebaseId = doc.getId();
                String issueIdString = doc.getString("id");
                
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("🔍 Document: " + firebaseId);
                System.out.println("   ID: " + issueIdString);
                
                // Chercher le signalement existant
                Optional<RoadIssue> optionalIssue = Optional.empty();
                
                // 1) Par firebaseId
                optionalIssue = roadIssueRepository.findByFirebaseId(firebaseId);
                
                // 2) Par UUID
                if (!optionalIssue.isPresent() && issueIdString != null) {
                    try {
                        UUID issueUuid = UUID.fromString(issueIdString);
                        optionalIssue = roadIssueRepository.findById(issueUuid);
                    } catch (IllegalArgumentException e) {
                        // UUID invalide, ignorer
                    }
                }
                
                if (optionalIssue.isPresent()) {
                    // ✅ Signalement EXISTANT - Mettre à jour
                    RoadIssue existing = optionalIssue.get();
                    System.out.println("   ✅ Existe déjà: " + existing.getTitle());
                    
                    // Mettre à jour le firebaseId si manquant
                    if (existing.getFirebaseId() == null) {
                        existing.setFirebaseId(firebaseId);
                        roadIssueRepository.save(existing);
                        System.out.println("   📝 Firebase ID ajouté");
                    }
                    
                    // Vérifier si le statusId a changé dans Firebase
                    Long firebaseStatusId = doc.getLong("statusId");
                    if (firebaseStatusId != null && !firebaseStatusId.equals(Long.valueOf(existing.getStatusId()))) {
                        System.out.println("   🔄 Status changé: " + existing.getStatusId() + " → " + firebaseStatusId);
                        existing.setStatusId(firebaseStatusId.intValue());
                        existing.setUpdatedAt(LocalDateTime.now());
                        roadIssueRepository.save(existing);
                        totalSynced++;
                    }
                    
                } else {
                    // ✅ NOUVEAU signalement - Le créer dans PostgreSQL
                    System.out.println("   🆕 Nouveau signalement à créer");
                    
                    RoadIssue newIssue = createRoadIssueFromFirebase(doc, firebaseId, issueIdString);
                    
                    if (newIssue != null) {
                        roadIssueRepository.save(newIssue);
                        totalSynced++;
                        System.out.println("   ✅ Créé: " + newIssue.getTitle());
                        
                        // Log de synchronisation
                        SyncLog log = new SyncLog();
                        log.setSyncType("PULL");
                        log.setEntity("road_issue");
                        log.setStatus("SUCCESS");
                        log.setMessage("Nouveau signalement créé: " + newIssue.getId());
                        syncLogRepository.save(log);
                    }
                }
            }
            
            System.out.println("\n=== FIN SYNCHRONISATION ===");
            System.out.println("✅ Total synchronisés: " + totalSynced + "\n");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur synchronisation: " + e.getMessage());
            e.printStackTrace();
            
            SyncLog log = new SyncLog();
            log.setSyncType("SYNC");
            log.setEntity("road_issue");
            log.setStatus("ERROR");
            log.setMessage(e.getMessage());
            syncLogRepository.save(log);
        }
        
        return totalSynced;
    }

    /**
     * Crée un RoadIssue à partir d'un document Firebase
     */
    private RoadIssue createRoadIssueFromFirebase(QueryDocumentSnapshot doc, String firebaseId, String issueIdString) {
        try {
            RoadIssue newIssue = new RoadIssue();
            
            // ID
            if (issueIdString != null) {
                try {
                    newIssue.setId(UUID.fromString(issueIdString));
                } catch (IllegalArgumentException e) {
                    newIssue.setId(UUID.randomUUID());
                }
            } else {
                newIssue.setId(UUID.randomUUID());
            }
            
            // Firebase ID
            newIssue.setFirebaseId(firebaseId);
            
            // Titre et description
            newIssue.setTitle(doc.getString("title"));
            newIssue.setDescription(doc.getString("description"));
            
            // Location (latitude/longitude -> Point PostGIS)
            Double latitude = doc.getDouble("latitude");
            Double longitude = doc.getDouble("longitude");
            
            if (latitude != null && longitude != null) {
                Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
                point.setSRID(4326);
                newIssue.setLocation(point);
                System.out.println("   📍 Location: " + latitude + ", " + longitude);
            } else {
                System.out.println("   ⚠️ Pas de coordonnées");
            }
            
            // Surface
            String surfaceStr = doc.getString("surfaceM2");
            if (surfaceStr != null) {
                try {
                    newIssue.setSurfaceM2(new BigDecimal(surfaceStr));
                } catch (NumberFormatException e) {
                    newIssue.setSurfaceM2(BigDecimal.ZERO);
                }
            }
            
            // Budget
            String budgetStr = doc.getString("budget");
            if (budgetStr != null) {
                try {
                    newIssue.setBudget(new BigDecimal(budgetStr));
                } catch (NumberFormatException e) {
                    newIssue.setBudget(BigDecimal.ZERO);
                }
            }
            
            // Status
            Long statusId = doc.getLong("statusId");
            newIssue.setStatusId(statusId != null ? statusId.intValue() : 1);
            
            // Company (optionnel)
            Long companyId = doc.getLong("companyId");
            if (companyId != null) {
                newIssue.setCompanyId(companyId.intValue());
            }
            
            // Reported By
            String reportedBy = doc.getString("reportedBy");
            if (reportedBy != null) {
                try {
                    newIssue.setReportedBy(UUID.fromString(reportedBy));
                } catch (IllegalArgumentException e) {
                    // reportedBy n'est pas un UUID valide (peut être un Firebase UID)
                    System.out.println("   ⚠️ reportedBy non UUID: " + reportedBy);
                }
            }
            
            // Dates
            Timestamp reportedAt = doc.getTimestamp("reportedAt");
            if (reportedAt != null) {
                newIssue.setReportedAt(reportedAt.toDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());
            } else {
                newIssue.setReportedAt(LocalDateTime.now());
            }
            
            Timestamp updatedAt = doc.getTimestamp("updatedAt");
            if (updatedAt != null) {
                newIssue.setUpdatedAt(updatedAt.toDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());
            } else {
                newIssue.setUpdatedAt(LocalDateTime.now());
            }
            
            // Marqué comme synchronisé
            newIssue.setIsSynced(true);
            
            return newIssue;
            
        } catch (Exception e) {
            System.err.println("   ❌ Erreur création: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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

    /**
     * PULL: Récupérer les signalements depuis Firebase et mettre à jour la base de données
     */
    @Transactional
    public int pullRoadIssuesFromFirebase() {
        int issuesPulled = 0;

        try {
            Firestore firestore = FirestoreClient.getFirestore(); // ✅ OK maintenant
            ApiFuture<QuerySnapshot> future = firestore.collection("road_issues").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            System.out.println("📥 Synchronisation de " + documents.size() + " signalements depuis Firebase...");

            for (QueryDocumentSnapshot doc : documents) {
                String firebaseDocId = doc.getId();
                String issueIdString = doc.getString("id");

                if (issueIdString == null) {
                    System.err.println("⚠️ Signalement sans ID: " + firebaseDocId);
                    continue;
                }

                UUID issueUuid;
                try {
                    issueUuid = UUID.fromString(issueIdString);
                } catch (IllegalArgumentException e) {
                    System.err.println("⚠️ UUID invalide: " + issueIdString);
                    continue;
                }

                // Vérifier si l'issue existe déjà
                Optional<RoadIssue> optionalIssue = roadIssueRepository.findById(issueUuid);

                if (optionalIssue.isPresent()) {
                    RoadIssue existingIssue = optionalIssue.get();
                    if (existingIssue.getFirebaseId() == null ||
                            !existingIssue.getFirebaseId().equals(firebaseDocId)) {
                        existingIssue.setFirebaseId(firebaseDocId);
                        roadIssueRepository.save(existingIssue);
                        System.out.println("✅ Firebase ID mis à jour: " + issueUuid + " -> " + firebaseDocId);
                    }
                    continue;
                }

                // Créer une nouvelle issue
                RoadIssue newIssue = new RoadIssue();
                newIssue.setId(issueUuid);
                newIssue.setFirebaseId(firebaseDocId);
                newIssue.setTitle(doc.getString("title"));
                newIssue.setDescription(doc.getString("description"));

                // ✅ Correction: La base utilise le type GEOGRAPHY, pas des colonnes séparées
                Double latitude = doc.getDouble("latitude");
                Double longitude = doc.getDouble("longitude");
                
                // Note: RoadIssue utilise probablement un champ "location" de type Point
                // Si vous avez des setters séparés, gardez ce code
                // Sinon, créez un objet Point (voir ci-dessous)
                if (latitude != null && longitude != null) {
                    // Option 1: Si vous avez setLatitude/setLongitude
                    // newIssue.setLatitude(latitude);
                    // newIssue.setLongitude(longitude);
                    
                    // Option 2: Si vous utilisez PostGIS Point
                    // Créer un Point (voir correction suivante)
                    String locationWkt = String.format("POINT(%f %f)", longitude, latitude);
                    newIssue.setLocationFromCoordinates(latitude, longitude);
                }

                // Surface et Budget
                String surfaceStr = doc.getString("surfaceM2");
                if (surfaceStr != null) {
                    try {
                        newIssue.setSurfaceM2(new BigDecimal(surfaceStr));
                    } catch (NumberFormatException e) {
                        newIssue.setSurfaceM2(BigDecimal.ZERO);
                    }
                }

                String budgetStr = doc.getString("budget");
                if (budgetStr != null) {
                    try {
                        newIssue.setBudget(new BigDecimal(budgetStr));
                    } catch (NumberFormatException e) {
                        newIssue.setBudget(BigDecimal.ZERO);
                    }
                }

                // Status
                Long statusId = doc.getLong("statusId");
                if (statusId != null) {
                    newIssue.setStatusId(statusId.intValue());
                } else {
                    newIssue.setStatusId(1);
                }

                // ✅ Correction: typeId n'existe pas dans RoadIssue
                // Supprimer ou commenter cette partie
                // Long typeId = doc.getLong("typeId");
                // if (typeId != null) {
                //     newIssue.setTypeId(typeId.intValue());
                // }

                // ✅ Correction: companyId est de type Integer dans RoadIssue, pas UUID
                Long companyId = doc.getLong("companyId");
                if (companyId != null) {
                    newIssue.setCompanyId(companyId.intValue());
                }

                // Utilisateur qui a créé le signalement
                String reportedBy = doc.getString("reportedBy");
                if (reportedBy != null) {
                    try {
                        newIssue.setReportedBy(UUID.fromString(reportedBy));
                    } catch (IllegalArgumentException e) {
                        System.err.println("⚠️ reportedBy UUID invalide: " + reportedBy);
                    }
                }

                // Dates
                com.google.cloud.Timestamp reportedAt = doc.getTimestamp("reportedAt");
                if (reportedAt != null) {
                    newIssue.setReportedAt(reportedAt.toDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDateTime());
                } else {
                    newIssue.setReportedAt(LocalDateTime.now());
                }

                com.google.cloud.Timestamp updatedAt = doc.getTimestamp("updatedAt");
                if (updatedAt != null) {
                    newIssue.setUpdatedAt(updatedAt.toDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDateTime());
                }

                roadIssueRepository.save(newIssue);
                issuesPulled++;
                System.out.println("✅ Signalement créé: " + newIssue.getTitle() +
                        " (ID: " + issueUuid + ", Firebase: " + firebaseDocId + ")");
            }

            System.out.println("✅ " + issuesPulled + " signalement(s) synchronisé(s)");

        } catch (Exception e) {
            System.err.println("❌ Erreur PULL road_issues: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur PULL road_issues", e);
        }

        return issuesPulled;
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
