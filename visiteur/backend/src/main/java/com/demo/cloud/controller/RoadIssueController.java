package com.demo.cloud.controller;

import com.demo.cloud.dto.RoadIssuePointResponse;
import com.demo.cloud.entity.RoadIssue;
import com.demo.cloud.entity.RoadIssueStatusHistory;
import com.demo.cloud.entity.IssueImage;
import com.demo.cloud.repository.RoadIssueRepository;
import com.demo.cloud.repository.RoadIssueStatusHistoryRepository;
import com.demo.cloud.repository.IssueImageRepository;
import com.demo.cloud.service.RoadIssueService;
import com.demo.cloud.service.RoadIssueStatusHistoryService;
import com.demo.cloud.service.RoadIssuesMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Tag(name = "RoadIssues", description = "Signalements routiers")
@CrossOrigin(origins = "*")
public class RoadIssueController {

    private final RoadIssueRepository roadIssueRepository;
    private final RoadIssueStatusHistoryService historyService;
    private final RoadIssueService roadIssueService;
    private final RoadIssueStatusHistoryRepository statusHistoryRepository;
    private final RoadIssuesMapService roadIssuesMapService;

    @Autowired
    private IssueImageRepository issueImageRepository;

    public RoadIssueController(
            RoadIssueRepository roadIssueRepository,
            RoadIssueStatusHistoryService historyService,
            RoadIssueService roadIssueService,
            RoadIssueStatusHistoryRepository statusHistoryRepository,
            RoadIssuesMapService roadIssuesMapService) {
        this.roadIssueRepository = roadIssueRepository;
        this.historyService = historyService;
        this.roadIssueService = roadIssueService;
        this.statusHistoryRepository = statusHistoryRepository;
        this.roadIssuesMapService = roadIssuesMapService;
    }

    @GetMapping("/issues")
    @Operation(summary = "Lister les signalements routiers")
    public List<RoadIssue> getAllIssues() {
        return roadIssueRepository.findAll();
    }

    @GetMapping("/map/issues")
    @Operation(summary = "Lister les signalements avec coordonnées pour la carte")
    public List<RoadIssuePointResponse> getIssuesForMap() {
        return roadIssuesMapService.getAllRoadIssuePoints();
    }

    @PutMapping("/issues/{id}")
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
        existing.setNiveau(updated.getNiveau()); // ✅ Mise à jour du niveau
        existing.setUpdatedAt(LocalDateTime.now());

        RoadIssue saved = roadIssueRepository.save(existing);

        if (statusChanged) {
            RoadIssueStatusHistory history = new RoadIssueStatusHistory();
            history.setRoadIssueId(saved.getId());
            history.setStatusId(saved.getStatusId());
            history.setChangedAt(LocalDateTime.now());
            statusHistoryRepository.save(history);
        }

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/issues/{id}/history")
    @Operation(summary = "Historique des changements de statut d'un signalement")
    public List<RoadIssueStatusHistory> getStatusHistory(@PathVariable UUID id) {
        return statusHistoryRepository.findByRoadIssueIdOrderByChangedAtAsc(id);
    }

    @GetMapping("/issues/{id}/images")
    @Operation(summary = "Récupérer les images d'un signalement")
    public ResponseEntity<List<IssueImage>> getIssueImages(@PathVariable UUID id) {
        try {
            List<IssueImage> images = issueImageRepository.findByRoadIssueId(id);
            System.out.println("Images trouvées pour issue " + id + ": " + images.size());
            return ResponseEntity.ok(images);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des images: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    @GetMapping("/photos/{issueId}/{filename}")
    @Operation(summary = "Servir une image depuis le dossier photos")
    public ResponseEntity<Resource> servePhoto(
            @PathVariable String issueId,
            @PathVariable String filename) {
        try {
            // Chemin: ../photos/{issueId}/{filename}
            Path filePath = Paths.get("../photos")
                .resolve(issueId)
                .resolve(filename)
                .normalize();
            
            System.out.println("Tentative de chargement de l'image: " + filePath.toAbsolutePath());
            
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "image/jpeg";
                }

                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
            } else {
                System.err.println("Image non trouvée: " + filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
