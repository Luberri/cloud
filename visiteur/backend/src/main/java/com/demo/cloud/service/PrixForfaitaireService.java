package com.demo.cloud.service;

import com.demo.cloud.entity.PrixForfaitaire;
import com.demo.cloud.repository.PrixForfaitaireRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PrixForfaitaireService {

    private final PrixForfaitaireRepository repository;

    public PrixForfaitaireService(PrixForfaitaireRepository repository) {
        this.repository = repository;
    }

    /**
     * Récupérer le prix forfaitaire actuel
     */
    public PrixForfaitaire getPrixActuel() {
        return repository.findById(1).orElseGet(() -> {
            // Créer un prix par défaut s'il n'existe pas
            PrixForfaitaire pf = new PrixForfaitaire();
            pf.setPrix(new BigDecimal("50000.00"));
            pf.setUpdatedAt(LocalDateTime.now());
            return repository.save(pf);
        });
    }

    /**
     * Mettre à jour le prix forfaitaire
     */
    @Transactional
    public PrixForfaitaire updatePrix(BigDecimal nouveauPrix) {
        PrixForfaitaire pf = getPrixActuel();
        pf.setPrix(nouveauPrix);
        pf.setUpdatedAt(LocalDateTime.now());
        return repository.save(pf);
    }

    /**
     * Calculer le budget estimé pour une surface donnée
     */
    public BigDecimal calculerBudget(BigDecimal surfaceM2) {
        PrixForfaitaire pf = getPrixActuel();
        return pf.calculerBudget(surfaceM2);
    }
}