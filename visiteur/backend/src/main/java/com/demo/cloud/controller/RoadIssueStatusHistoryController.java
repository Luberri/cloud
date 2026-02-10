package com.demo.cloud.controller;

import com.demo.cloud.dto.StatusHistoryResponse;
import com.demo.cloud.service.RoadIssueStatusHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/issues/{issueId}/history")
@Tag(name = "Status History", description = "Historique des changements de statut")
@CrossOrigin(origins = "*")
public class RoadIssueStatusHistoryController {

    private final RoadIssueStatusHistoryService historyService;

    public RoadIssueStatusHistoryController(RoadIssueStatusHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    @Operation(summary = "Obtenir l'historique des statuts d'un signalement")
    public ResponseEntity<List<StatusHistoryResponse>> getHistory(@PathVariable UUID issueId) {
        return ResponseEntity.ok(historyService.getHistoryByRoadIssue(issueId));
    }

    @PostMapping
    @Operation(summary = "Ajouter un changement de statut")
    public ResponseEntity<StatusHistoryResponse> addStatusChange(
            @PathVariable UUID issueId,
            @RequestParam Integer statusId,
            @RequestParam(required = false) UUID changedBy) {
        
        var history = historyService.addStatusChange(issueId, statusId, changedBy);
        
        return ResponseEntity.ok(new StatusHistoryResponse(
            history.getId(),
            history.getRoadIssueId(),
            history.getStatusId(),
            null, null,
            history.getChangedAt(),
            history.getChangedBy(),
            null
        ));
    }
}