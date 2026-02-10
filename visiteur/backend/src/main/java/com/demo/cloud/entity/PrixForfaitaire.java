package com.demo.cloud.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prix_forfaitaire")
public class PrixForfaitaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "prix", nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructeurs
    public PrixForfaitaire() {
        this.prix = new BigDecimal("50000.00");
        this.updatedAt = LocalDateTime.now();
    }

    public PrixForfaitaire(BigDecimal prix) {
        this.prix = prix;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters et Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Méthode utilitaire pour calculer le budget
    public BigDecimal calculerBudget(BigDecimal surfaceM2) {
        if (surfaceM2 == null || prix == null) {
            return BigDecimal.ZERO;
        }
        return prix.multiply(surfaceM2);
    }
}