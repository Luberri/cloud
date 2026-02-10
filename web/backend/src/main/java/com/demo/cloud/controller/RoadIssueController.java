package com.demo.cloud.controller;

import com.demo.cloud.entity.RoadIssue;
import com.demo.cloud.repository.RoadIssueRepository;
import com.demo.cloud.service.RoadIssueService;
import com.demo.cloud.service.RoadIssueStatusHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/issues")
@Tag(name = "RoadIssues", description = "Signalements routiers")
@CrossOrigin(origins = "*")
public class RoadIssueController {

    private final RoadIssueRepository roadIssueRepository;
    private final RoadIssueStatusHistoryService historyService;
    private final RoadIssueService roadIssueService;

    public RoadIssueController(
            RoadIssueRepository roadIssueRepository,
            RoadIssueStatusHistoryService historyService,
            RoadIssueService roadIssueService) {
        this.roadIssueRepository = roadIssueRepository;
        this.historyService = historyService;
        this.roadIssueService = roadIssueService;
    }

    @GetMapping
    @Operation(summary = "Lister les signalements routiers")
    public List<RoadIssue> getAllIssues() {
        return roadIssueRepository.findAll();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un signalement routier")
    public ResponseEntity<RoadIssue> updateIssue(
            @PathVariable UUID id,
            @RequestBody RoadIssue updated,
            @RequestParam(required = false) UUID changedBy) {

        RoadIssue existing = roadIssueRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Signalement introuvable"));

        boolean statusChanged = updated.getStatusId() != null
            && !updated.getStatusId().equals(existing.getStatusId());

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setSurfaceM2(updated.getSurfaceM2());
        existing.setBudget(updated.getBudget());
        existing.setStatusId(updated.getStatusId());
        existing.setUpdatedAt(LocalDateTime.now());

        RoadIssue saved = roadIssueRepository.save(existing);

        if (statusChanged) {
            historyService.addStatusChange(id, updated.getStatusId(), changedBy);
        }

        // PUSH instantané vers Firestore (update du doc existant)
        roadIssueService.pushSingleIssue(saved);

        return ResponseEntity.ok(saved);
    }
}
