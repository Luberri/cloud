package com.demo.cloud.controller;

import com.demo.cloud.service.ImageSyncService;
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

    public SyncController(RoadIssueService roadIssueService,
                         UserSyncService userSyncService,
                         ImageSyncService imageSyncService) {
        this.roadIssueService = roadIssueService;
        this.userSyncService = userSyncService;
        this.imageSyncService = imageSyncService;
    }

    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        Map<String, Object> result = new HashMap<>();

        // 1) PUSH local -> Firebase (Auth)
        int usersPushed = userSyncService.pushLocalUsersToFirebaseAuth();

        // 2) PUSH local -> Firebase (Firestore) + vérification status_id
        int roadIssuesSynced = roadIssueService.syncWithFirebase();

        // 3) PULL Firebase -> local (utilisateurs)
        int usersPulled = userSyncService.syncUsersFromFirebase();

        // 4) Synchronisation bidirectionnelle des images
        Map<String, Integer> imagesSyncResult = imageSyncService.syncImages();

        result.put("usersPushed", usersPushed);
        result.put("roadIssuesSynced", roadIssuesSynced);
        result.put("usersPulled", usersPulled);
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
}