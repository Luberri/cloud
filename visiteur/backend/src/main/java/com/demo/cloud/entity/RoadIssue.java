package com.demo.cloud.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "road_issues")
public class RoadIssue {

    @Id
    private UUID id;

    private String title;
    private String description;

    @JsonIgnore
    @Column(name = "location", columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Column(name = "surface_m2")
    private BigDecimal surfaceM2;

    private BigDecimal budget;

    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "reported_by")
    private UUID reportedBy;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt;

    @Column(name = "is_synced")
    private Boolean isSynced = false;

    @Column(name = "firebase_id")
    private String firebaseId;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ Nouveau champ niveau (1 à 10)
    @Column(name = "niveau")
    private Integer niveau = 1;

    // ✅ Méthode helper pour définir les coordonnées
    public void setLocationFromCoordinates(double latitude, double longitude) {
        GeometryFactory geometryFactory = new GeometryFactory();
        this.location = geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }

    // ✅ Méthodes pour extraire latitude/longitude
    public Double getLatitude() {
        return location != null ? location.getY() : null;
    }

    public Double getLongitude() {
        return location != null ? location.getX() : null;
    }

    // Getters et setters existants...
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }

    public BigDecimal getSurfaceM2() { return surfaceM2; }
    public void setSurfaceM2(BigDecimal surfaceM2) { this.surfaceM2 = surfaceM2; }

    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }

    public Integer getStatusId() { return statusId; }
    public void setStatusId(Integer statusId) { this.statusId = statusId; }

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public UUID getReportedBy() { return reportedBy; }
    public void setReportedBy(UUID reportedBy) { this.reportedBy = reportedBy; }

    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }

    public Boolean getIsSynced() { return isSynced; }
    public void setIsSynced(Boolean isSynced) { this.isSynced = isSynced; }

    public String getFirebaseId() { return firebaseId; }
    public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ✅ Getter et Setter pour niveau
    public Integer getNiveau() { return niveau; }
    public void setNiveau(Integer niveau) { 
        if (niveau != null && niveau >= 1 && niveau <= 10) {
            this.niveau = niveau;
        } else {
            this.niveau = 1; // Valeur par défaut
        }
    }
}
