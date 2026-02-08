package com.demo.cloud.controller;

import com.demo.cloud.dto.CreateImageIssueRequest;
import com.demo.cloud.dto.ImageIssueResponse;
import com.demo.cloud.service.ImageIssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/issues/{issueId}/images")
@Tag(name = "Issue Images", description = "Gestion des images des signalements")
@CrossOrigin(origins = "*")
public class ImageIssueController {

    private final ImageIssueService imageIssueService;

    public ImageIssueController(ImageIssueService imageIssueService) {
        this.imageIssueService = imageIssueService;
    }

    @GetMapping
    @Operation(summary = "Obtenir les images d'un signalement")
    public ResponseEntity<List<ImageIssueResponse>> getImages(@PathVariable UUID issueId) {
        return ResponseEntity.ok(imageIssueService.getImagesByRoadIssue(issueId));
    }

    @PostMapping
    @Operation(summary = "Ajouter une image à un signalement")
    public ResponseEntity<ImageIssueResponse> addImage(
            @PathVariable UUID issueId,
            @RequestBody CreateImageIssueRequest request) {
        
        // S'assurer que le roadIssueId correspond
        CreateImageIssueRequest finalRequest = new CreateImageIssueRequest(
            issueId,
            request.storagePath(),
            request.downloadUrl(),
            request.thumbnailUrl()
        );
        
        return ResponseEntity.ok(imageIssueService.addImage(finalRequest));
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Supprimer une image")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID issueId,
            @PathVariable UUID imageId) {
        imageIssueService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Supprimer toutes les images d'un signalement")
    public ResponseEntity<Void> deleteAllImages(@PathVariable UUID issueId) {
        imageIssueService.deleteAllImagesForRoadIssue(issueId);
        return ResponseEntity.noContent().build();
    }
}