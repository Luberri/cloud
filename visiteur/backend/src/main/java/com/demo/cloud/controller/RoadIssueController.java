package com.demo.cloud.controller;

import com.demo.cloud.entity.RoadIssue;
import com.demo.cloud.entity.RoadIssueStatusHistory;
import com.demo.cloud.repository.RoadIssueRepository;
import com.demo.cloud.repository.RoadIssueStatusHistoryRepository;
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
    private final RoadIssueStatusHistoryRepository statusHistoryRepository;

    public RoadIssueController(RoadIssueRepository roadIssueRepository,
                               RoadIssueStatusHistoryRepository statusHistoryRepository) {
        this.roadIssueRepository = roadIssueRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @GetMapping
    @Operation(summary = "Lister les signalements routiers")
    public List<RoadIssue> getAllIssues() {
        return roadIssueRepository.findAll();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un signalement routier")
    public ResponseEntity<RoadIssue> updateIssue(@PathVariable UUID id, @RequestBody RoadIssue updated) {
        RoadIssue existing = roadIssueRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Signalement introuvable"));

        // Détecter le changement de statut pour l'historique
        boolean statusChanged = updated.getStatusId() != null
            && !updated.getStatusId().equals(existing.getStatusId());

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setSurfaceM2(updated.getSurfaceM2());
        existing.setBudget(updated.getBudget());
        existing.setStatusId(updated.getStatusId());
        existing.setUpdatedAt(LocalDateTime.now());

        RoadIssue saved = roadIssueRepository.save(existing);

        // Enregistrer dans l'historique si le statut a changé
        if (statusChanged) {
            RoadIssueStatusHistory history = new RoadIssueStatusHistory();
            history.setRoadIssueId(saved.getId());
            history.setStatusId(saved.getStatusId());
            history.setChangedAt(LocalDateTime.now());
            statusHistoryRepository.save(history);
        }

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Historique des changements de statut d'un signalement")
    public List<RoadIssueStatusHistory> getStatusHistory(@PathVariable UUID id) {
        return statusHistoryRepository.findByRoadIssueIdOrderByChangedAtAsc(id);
    }
}
