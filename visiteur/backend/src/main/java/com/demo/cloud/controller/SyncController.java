package com.demo.cloud.controller;

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

    public SyncController(RoadIssueService roadIssueService, UserSyncService userSyncService) {
        this.roadIssueService = roadIssueService;
        this.userSyncService = userSyncService;
    }

    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        Map<String, Object> result = new HashMap<>();

        // 1) PUSH local -> Firebase (Auth)
        int usersPushed = userSyncService.pushLocalUsersToFirebaseAuth();

        // 2) PUSH local -> Firebase (Firestore)
        int roadIssuesPushed = roadIssueService.syncWithFirebase();

        // 3) (optionnel) PULL Firebase -> local
        int usersPulled = userSyncService.syncUsersFromFirebase();

        result.put("usersPushed", usersPushed);
        result.put("roadIssues", roadIssuesPushed);
        result.put("usersPulled", usersPulled);
        return ResponseEntity.ok(result);
    }
}