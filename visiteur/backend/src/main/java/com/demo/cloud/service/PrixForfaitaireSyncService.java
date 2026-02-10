package com.demo.cloud.service;

import com.demo.cloud.entity.PrixForfaitaire;
import com.demo.cloud.repository.PrixForfaitaireRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PrixForfaitaireSyncService {

    private final PrixForfaitaireRepository repository;
    private static final String COLLECTION = "prix_forfaitaire";
    private static final String DOC_ID = "config";

    public PrixForfaitaireSyncService(PrixForfaitaireRepository repository) {
        this.repository = repository;
    }

    /**
     * PUSH: Envoyer le prix forfaitaire local vers Firebase
     */
    @Transactional
    public boolean pushToFirebase() {
        try {
            Firestore db = FirestoreClient.getFirestore();
            PrixForfaitaire local = repository.findById(1)
                .orElseThrow(() -> new RuntimeException("Prix forfaitaire introuvable"));

            Map<String, Object> data = new HashMap<>();
            data.put("prix_par_m2", local.getPrix().doubleValue());
            data.put("updated_at", local.getUpdatedAt() != null ? 
                Date.from(local.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant()) : 
                new Date());

            ApiFuture<WriteResult> future = db.collection(COLLECTION)
                .document(DOC_ID)
                .set(data);

            future.get(); // Attendre la complétion
            System.out.println("✅ Prix forfaitaire synchronisé vers Firebase: " + local.getPrix());
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur PUSH prix forfaitaire vers Firebase: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * PULL: Récupérer le prix forfaitaire depuis Firebase
     */
    @Transactional
    public boolean pullFromFirebase() {
        try {
            Firestore db = FirestoreClient.getFirestore();
            ApiFuture<DocumentSnapshot> future = db.collection(COLLECTION)
                .document(DOC_ID)
                .get();

            DocumentSnapshot doc = future.get();

            if (!doc.exists()) {
                System.out.println("⚠️ Document prix_forfaitaire/config n'existe pas dans Firebase");
                return false;
            }

            Double prixParM2 = doc.getDouble("prix_par_m2");
            com.google.cloud.Timestamp updatedAtTs = doc.getTimestamp("updated_at");

            if (prixParM2 == null) {
                System.err.println("❌ Le champ 'prix_par_m2' est manquant dans Firebase");
                return false;
            }

            // Récupérer ou créer l'entité locale
            PrixForfaitaire local = repository.findById(1).orElse(new PrixForfaitaire());
            
            local.setPrix(BigDecimal.valueOf(prixParM2));
            
            if (updatedAtTs != null) {
                local.setUpdatedAt(LocalDateTime.ofInstant(
                    updatedAtTs.toDate().toInstant(),
                    ZoneId.systemDefault()
                ));
            } else {
                local.setUpdatedAt(LocalDateTime.now());
            }

            repository.save(local);
            System.out.println("✅ Prix forfaitaire récupéré depuis Firebase: " + local.getPrix());
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur PULL prix forfaitaire depuis Firebase: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Synchronisation bidirectionnelle (compare les dates)
     */
    @Transactional
    public String syncBidirectional() {
        try {
            Firestore db = FirestoreClient.getFirestore();
            
            // Récupérer local
            PrixForfaitaire local = repository.findById(1).orElse(null);
            
            // Récupérer Firebase
            ApiFuture<DocumentSnapshot> future = db.collection(COLLECTION)
                .document(DOC_ID)
                .get();
            DocumentSnapshot doc = future.get();

            // Si pas de données locales, faire un PULL
            if (local == null) {
                if (doc.exists()) {
                    pullFromFirebase();
                    return "PULL effectué (aucune donnée locale)";
                } else {
                    // Créer avec valeur par défaut
                    local = new PrixForfaitaire(new BigDecimal("50000.00"));
                    repository.save(local);
                    pushToFirebase();
                    return "PUSH effectué (création initiale)";
                }
            }

            // Si pas de données Firebase, faire un PUSH
            if (!doc.exists()) {
                pushToFirebase();
                return "PUSH effectué (aucune donnée Firebase)";
            }

            // Comparer les dates de mise à jour
            com.google.cloud.Timestamp firebaseTs = doc.getTimestamp("updated_at");
            LocalDateTime localUpdated = local.getUpdatedAt();

            if (firebaseTs == null) {
                pushToFirebase();
                return "PUSH effectué (pas de date Firebase)";
            }

            LocalDateTime firebaseUpdated = LocalDateTime.ofInstant(
                firebaseTs.toDate().toInstant(),
                ZoneId.systemDefault()
            );

            if (localUpdated == null) {
                pullFromFirebase();
                return "PULL effectué (pas de date locale)";
            }

            // Synchroniser la version la plus récente
            if (firebaseUpdated.isAfter(localUpdated)) {
                pullFromFirebase();
                return "PULL effectué (Firebase plus récent)";
            } else if (localUpdated.isAfter(firebaseUpdated)) {
                pushToFirebase();
                return "PUSH effectué (Local plus récent)";
            } else {
                return "Déjà synchronisé (dates identiques)";
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur sync bidirectionnel: " + e.getMessage());
            return "Erreur: " + e.getMessage();
        }
    }
}