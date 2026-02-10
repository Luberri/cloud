package com.demo.cloud.service;

import com.demo.cloud.entity.RoadIssue;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class FirebaseService {

    // IMPORTANT: ta collection Firestore
    public static final String ROAD_ISSUES_COLLECTION = "road_issues";

    public record FirestoreRoadIssue(String docId, Map<String, Object> data) {}

    /**
     * PUSH: DB locale -> Firestore
     * @return firestore document id (firebaseId)
     */
    public String pushRoadIssue(RoadIssue issue) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            DocumentReference docRef =
                    (issue.getFirebaseId() != null && !issue.getFirebaseId().isBlank())
                            ? db.collection(ROAD_ISSUES_COLLECTION).document(issue.getFirebaseId())
                            : db.collection(ROAD_ISSUES_COLLECTION).document();

            Map<String, Object> data = new HashMap<>();
            data.put("id", issue.getId() != null ? issue.getId().toString() : null);
            data.put("title", issue.getTitle());
            data.put("description", issue.getDescription());

            data.put("latitude", issue.getLatitude());
            data.put("longitude", issue.getLongitude());

            data.put("surfaceM2", issue.getSurfaceM2());
            data.put("budget", issue.getBudget());
            data.put("statusId", issue.getStatusId());
            data.put("companyId", issue.getCompanyId());

            data.put("reportedBy", issue.getReportedBy() != null ? issue.getReportedBy().toString() : null);
            data.put("reportedAt", toDate(issue.getReportedAt()));
            data.put("updatedAt", toDate(issue.getUpdatedAt()));

            // MERGE pour éviter d'écraser des champs côté Firestore
            ApiFuture<WriteResult> write = docRef.set(data, SetOptions.merge());
            write.get(); // force l'exécution / remonte les erreurs

            return docRef.getId();
        } catch (Exception e) {
            // IMPORTANT: ne pas avaler, sinon RoadIssueService voit juste "firebaseId vide"
            throw new RuntimeException("Firestore pushRoadIssue failed: " + e.getMessage(), e);
        }
    }

    /**
     * PULL: Firestore -> (retourne tous les documents bruts)
     */
    public List<FirestoreRoadIssue> fetchRoadIssues() {
        try {
            Firestore db = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> future = db.collection(ROAD_ISSUES_COLLECTION).get();

            List<FirestoreRoadIssue> out = new ArrayList<>();
            for (QueryDocumentSnapshot doc : future.get().getDocuments()) {
                out.add(new FirestoreRoadIssue(doc.getId(), doc.getData()));
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Firestore fetchRoadIssues failed: " + e.getMessage(), e);
        }
    }

    private static Date toDate(LocalDateTime ldt) {
        if (ldt == null) return null;
        return Date.from(ldt.toInstant(ZoneOffset.UTC));
    }
}