package com.demo.cloud.repository;

import com.demo.cloud.entity.PrixForfaitaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrixForfaitaireRepository extends JpaRepository<PrixForfaitaire, Integer> {
    
    // Récupérer le prix forfaitaire actuel (le plus récent)
    @Query("SELECT p FROM PrixForfaitaire p ORDER BY p.updatedAt DESC LIMIT 1")
    Optional<PrixForfaitaire> findLatest();
    
    // Récupérer le premier enregistrement (il n'y en a normalement qu'un seul)
    default PrixForfaitaire getOrCreate() {
        return findById(1).orElseGet(() -> {
            PrixForfaitaire pf = new PrixForfaitaire();
            return pf;
        });
    }
}