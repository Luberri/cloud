package com.demo.cloud.controller;

import com.demo.cloud.entity.RoadIssue;
import com.demo.cloud.repository.RoadIssueRepository;
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

    public RoadIssueController(RoadIssueRepository roadIssueRepository) {
        this.roadIssueRepository = roadIssueRepository;
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

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setSurfaceM2(updated.getSurfaceM2());
        existing.setBudget(updated.getBudget());
        existing.setStatusId(updated.getStatusId());
        existing.setUpdatedAt(LocalDateTime.now());

        RoadIssue saved = roadIssueRepository.save(existing);
        return ResponseEntity.ok(saved);
    }
}
