package com.demo.cloud.controller;

import com.demo.cloud.entity.PrixForfaitaire;
import com.demo.cloud.service.PrixForfaitaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/prix-forfaitaire")
@Tag(name = "Prix Forfaitaire", description = "Gestion du prix forfaitaire par m²")
@CrossOrigin(origins = "*")
public class PrixForfaitaireController {

    private final PrixForfaitaireService service;

    public PrixForfaitaireController(PrixForfaitaireService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Récupérer le prix forfaitaire actuel")
    public ResponseEntity<PrixForfaitaire> getPrixActuel() {
        return ResponseEntity.ok(service.getPrixActuel());
    }

    @PutMapping
    @Operation(summary = "Mettre à jour le prix forfaitaire")
    public ResponseEntity<PrixForfaitaire> updatePrix(@RequestBody Map<String, Object> body) {
        BigDecimal nouveauPrix;
        
        Object prixValue = body.get("prix");
        if (prixValue instanceof Number) {
            nouveauPrix = new BigDecimal(prixValue.toString());
        } else if (prixValue instanceof String) {
            nouveauPrix = new BigDecimal((String) prixValue);
        } else {
            return ResponseEntity.badRequest().build();
        }

        if (nouveauPrix.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }

        PrixForfaitaire updated = service.updatePrix(nouveauPrix);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/calculer")
    @Operation(summary = "Calculer le budget pour une surface donnée")
    public ResponseEntity<Map<String, Object>> calculerBudget(@RequestParam BigDecimal surfaceM2) {
        PrixForfaitaire pf = service.getPrixActuel();
        BigDecimal budget = service.calculerBudget(surfaceM2);
        
        return ResponseEntity.ok(Map.of(
            "surfaceM2", surfaceM2,
            "prixParM2", pf.getPrix(),
            "budgetEstime", budget
        ));
    }
}