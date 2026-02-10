package com.demo.cloud.controller;

import com.demo.cloud.service.ImageSyncService;
import com.demo.cloud.service.PrixForfaitaireSyncService;
import com.demo.cloud.service.RoadIssueService;
import com.demo.cloud.service.UserSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/sync")
@CrossOrigin(origins = "*")
public class SyncController {

    private final RoadIssueService roadIssueService;
    private final UserSyncService userSyncService;
    private final ImageSyncService imageSyncService;
    private final PrixForfaitaireSyncService prixSyncService;

    public SyncController(RoadIssueService roadIssueService,
                         UserSyncService userSyncService,
                         ImageSyncService imageSyncService,
                         PrixForfaitaireSyncService prixSyncService) {
        this.roadIssueService = roadIssueService;
        this.userSyncService = userSyncService;
        this.imageSyncService = imageSyncService;
        this.prixSyncService = prixSyncService;
    }

    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        Map<String, Object> result = new HashMap<>();

        // 1) PUSH local -> Firebase (Auth)
        int usersPushed = userSyncService.pushLocalUsersToFirebaseAuth();

        // 2) PUSH local -> Firebase (Firestore) + vérification status_id et niveau
        int roadIssuesSynced = roadIssueService.syncWithFirebase();

        // 3) PULL Firebase -> local (utilisateurs)
        int usersPulled = userSyncService.syncUsersFromFirebase();

        // 4) Synchronisation bidirectionnelle des images
        Map<String, Integer> imagesSyncResult = imageSyncService.syncImages();

        // 5) ✅ Synchronisation du prix forfaitaire
        String prixSyncResult = prixSyncService.syncBidirectional();

        result.put("usersPushed", usersPushed);
        result.put("roadIssuesSynced", roadIssuesSynced);
        result.put("usersPulled", usersPulled);
        result.put("prixForfaitaireSync", prixSyncResult);
        result.putAll(imagesSyncResult); // imagesPulled et imagesPushed

        return ResponseEntity.ok(result);
    }

    @PostMapping("/status-changes")
    public ResponseEntity<Map<String, Object>> syncStatusChanges() {
        Map<String, Object> result = new HashMap<>();

        int statusChangesSynced = roadIssueService.syncStatusChangesToFirebase();

        result.put("statusChangesSynced", statusChangesSynced);
        return ResponseEntity.ok(result);
    }

    // Endpoint spécifique pour la synchronisation des images
    @PostMapping("/images")
    public ResponseEntity<Map<String, Integer>> syncImages() {
        Map<String, Integer> result = imageSyncService.syncImages();
        return ResponseEntity.ok(result);
    }

    // PULL uniquement les images depuis Firebase
    @PostMapping("/images/pull")
    public ResponseEntity<Map<String, Integer>> pullImages() {
        Map<String, Integer> result = new HashMap<>();
        int pulled = imageSyncService.pullImagesFromFirebase();
        result.put("imagesPulled", pulled);
        return ResponseEntity.ok(result);
    }

    // PUSH uniquement les images vers Firebase
    @PostMapping("/images/push")
    public ResponseEntity<Map<String, Integer>> pushImages() {
        Map<String, Integer> result = new HashMap<>();
        int pushed = imageSyncService.pushImagesToFirebase();
        result.put("imagesPushed", pushed);
        return ResponseEntity.ok(result);
    }

    // ✅ NOUVEAUX ENDPOINTS pour prix forfaitaire
    @PostMapping("/prix-forfaitaire/push")
    public ResponseEntity<Map<String, Object>> pushPrixForfaitaire() {
        Map<String, Object> result = new HashMap<>();
        boolean success = prixSyncService.pushToFirebase();
        result.put("success", success);
        result.put("message", success ? "Prix forfaitaire envoyé vers Firebase" : "Échec du PUSH");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/prix-forfaitaire/pull")
    public ResponseEntity<Map<String, Object>> pullPrixForfaitaire() {
        Map<String, Object> result = new HashMap<>();
        boolean success = prixSyncService.pullFromFirebase();
        result.put("success", success);
        result.put("message", success ? "Prix forfaitaire récupéré depuis Firebase" : "Échec du PULL");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/prix-forfaitaire")
    public ResponseEntity<Map<String, Object>> syncPrixForfaitaire() {
        Map<String, Object> result = new HashMap<>();
        String syncResult = prixSyncService.syncBidirectional();
        result.put("result", syncResult);
        return ResponseEntity.ok(result);
    }

    // ✅ PULL complet depuis Firebase (road_issues + prix_forfaitaire)
    @PostMapping("/pull-all")
    public ResponseEntity<Map<String, Object>> pullAll() {
        Map<String, Object> result = new HashMap<>();
        
        int roadIssuesPulled = roadIssueService.pullFromFirebase();
        boolean prixPulled = prixSyncService.pullFromFirebase();
        int imagesPulled = imageSyncService.pullImagesFromFirebase();
        
        result.put("roadIssuesPulled", roadIssuesPulled);
        result.put("prixForfaitairePulled", prixPulled);
        result.put("imagesPulled", imagesPulled);
        
        return ResponseEntity.ok(result);
    }
}