package com.demo.cloud.service;

import com.demo.cloud.entity.RoadIssue;
import com.demo.cloud.repository.RoadIssueRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class RoadIssueService {

    private final RoadIssueRepository roadIssueRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public RoadIssueService(RoadIssueRepository roadIssueRepository) {
        this.roadIssueRepository = roadIssueRepository;
    }

    /**
     * PUSH: Synchroniser les signalements locaux vers Firebase
     * ✅ Inclut maintenant le champ 'niveau'
     */
    @Transactional
    public int syncWithFirebase() {
        Firestore db = FirestoreClient.getFirestore();
        List<RoadIssue> localIssues = roadIssueRepository.findAll();
        int synced = 0;

        for (RoadIssue issue : localIssues) {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("title", issue.getTitle());
                data.put("description", issue.getDescription());
                data.put("latitude", issue.getLatitude());
                data.put("longitude", issue.getLongitude());
                data.put("surface_m2", issue.getSurfaceM2() != null ? issue.getSurfaceM2().doubleValue() : null);
                data.put("budget", issue.getBudget() != null ? issue.getBudget().doubleValue() : null);
                data.put("status_id", issue.getStatusId());
                data.put("niveau", issue.getNiveau() != null ? issue.getNiveau() : 1); // ✅ Ajout du niveau
                data.put("company_id", issue.getCompanyId());
                data.put("reported_by", issue.getReportedBy() != null ? issue.getReportedBy().toString() : null);
                data.put("reported_at", issue.getReportedAt() != null ? 
                    Date.from(issue.getReportedAt().atZone(ZoneId.systemDefault()).toInstant()) : null);
                data.put("updated_at", new Date());

                String docId = issue.getFirebaseId();
                if (docId == null || docId.isEmpty()) {
                    // Créer un nouveau document
                    ApiFuture<DocumentReference> future = db.collection("road_issues").add(data);
                    DocumentReference ref = future.get();
                    issue.setFirebaseId(ref.getId());
                } else {
                    // Mettre à jour le document existant
                    db.collection("road_issues").document(docId).set(data, SetOptions.merge()).get();
                }

                issue.setIsSynced(true);
                issue.setUpdatedAt(LocalDateTime.now());
                roadIssueRepository.save(issue);
                synced++;

            } catch (Exception e) {
                System.err.println("Erreur sync Firebase pour issue " + issue.getId() + ": " + e.getMessage());
            }
        }

        System.out.println("✅ " + synced + " signalements synchronisés vers Firebase");
        return synced;
    }

    /**
     * PULL: Synchroniser les signalements depuis Firebase vers local
     * ✅ Inclut maintenant le champ 'niveau'
     */
    @Transactional
    public int pullFromFirebase() {
        Firestore db = FirestoreClient.getFirestore();
        int pulled = 0;

        try {
            ApiFuture<QuerySnapshot> future = db.collection("road_issues").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (DocumentSnapshot doc : documents) {
                try {
                    String firebaseId = doc.getId();
                    
                    // Vérifier si existe déjà
                    Optional<RoadIssue> existingOpt = roadIssueRepository.findByFirebaseId(firebaseId);
                    RoadIssue issue = existingOpt.orElse(new RoadIssue());

                    // Mapper les données Firebase vers l'entité
                    issue.setFirebaseId(firebaseId);
                    issue.setTitle(doc.getString("title"));
                    issue.setDescription(doc.getString("description"));

                    // Géolocalisation
                    Double lat = doc.getDouble("latitude");
                    Double lon = doc.getDouble("longitude");
                    if (lat != null && lon != null) {
                        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
                        issue.setLocation(point);
                    }

                    // Données numériques
                    Double surfaceM2 = doc.getDouble("surface_m2");
                    issue.setSurfaceM2(surfaceM2 != null ? BigDecimal.valueOf(surfaceM2) : null);

                    Double budget = doc.getDouble("budget");
                    issue.setBudget(budget != null ? BigDecimal.valueOf(budget) : null);

                    // Statut et niveau
                    issue.setStatusId(doc.getLong("status_id") != null ? doc.getLong("status_id").intValue() : 1);
                    
                    // ✅ Récupérer le niveau depuis Firebase
                    Long niveauLong = doc.getLong("niveau");
                    issue.setNiveau(niveauLong != null ? niveauLong.intValue() : 1);

                    issue.setCompanyId(doc.getLong("company_id") != null ? doc.getLong("company_id").intValue() : null);

                    // Dates
                    com.google.cloud.Timestamp reportedAtTs = doc.getTimestamp("reported_at");
                    if (reportedAtTs != null) {
                        issue.setReportedAt(LocalDateTime.ofInstant(
                            reportedAtTs.toDate().toInstant(), 
                            ZoneId.systemDefault()
                        ));
                    }

                    issue.setIsSynced(true);
                    issue.setUpdatedAt(LocalDateTime.now());

                    roadIssueRepository.save(issue);
                    pulled++;

                } catch (Exception e) {
                    System.err.println("Erreur traitement document " + doc.getId() + ": " + e.getMessage());
                }
            }

            System.out.println("✅ " + pulled + " signalements récupérés depuis Firebase");

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Erreur lors du PULL depuis Firebase: " + e.getMessage());
        }

        return pulled;
    }

    /**
     * Synchroniser les changements de statut vers Firebase
     */
    @Transactional
    public int syncStatusChangesToFirebase() {
        Firestore db = FirestoreClient.getFirestore();
        List<RoadIssue> issues = roadIssueRepository.findAll();
        int synced = 0;

        for (RoadIssue issue : issues) {
            if (issue.getFirebaseId() != null && !issue.getFirebaseId().isEmpty()) {
                try {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status_id", issue.getStatusId());
                    updates.put("niveau", issue.getNiveau() != null ? issue.getNiveau() : 1); // ✅ Inclure niveau
                    updates.put("updated_at", new Date());

                    db.collection("road_issues")
                        .document(issue.getFirebaseId())
                        .update(updates)
                        .get();

                    synced++;
                } catch (Exception e) {
                    System.err.println("Erreur sync statut pour " + issue.getId() + ": " + e.getMessage());
                }
            }
        }

        System.out.println("✅ " + synced + " changements de statut synchronisés");
        return synced;
    }
}
