package com.demo.cloud.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "road_issues")
public class RoadIssue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(length = 150)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // Location stocké comme String au format "POINT(longitude latitude)"
    @Column(name = "location", columnDefinition = "geography(Point, 4326)", nullable = false)
    @ColumnTransformer(
            read = "ST_AsText(location)",          // DB -> String "POINT(lon lat)"
            write = "ST_GeogFromText(?)"           // String -> geography
    )
    private String location;
    
    @Column(name = "surface_m2", precision = 10, scale = 2)
    private BigDecimal surfaceM2;
    
    @Column(precision = 14, scale = 2)
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
    
    @Column(name = "firebase_id", length = 150)
    private String firebaseId;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructeurs
    public RoadIssue() {
        this.reportedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Méthodes pour extraire latitude et longitude depuis location
    public Double getLatitude() {
        double[] lonLat = parseLonLatFromLocation(this.location);
        return lonLat == null ? null : lonLat[1];
    }

    public Double getLongitude() {
        double[] lonLat = parseLonLatFromLocation(this.location);
        return lonLat == null ? null : lonLat[0];
    }

    /**
     * Supporte:
     * - WKT: "POINT(lon lat)"
     * - EWKB hex (PostGIS): "0101000020E6100000..." (Point avec SRID)
     */
    private static double[] parseLonLatFromLocation(String location) {
        if (location == null || location.isBlank()) return null;

        String loc = location.trim();

        // 1) WKT: POINT(lon lat)
        if (loc.regionMatches(true, 0, "POINT(", 0, "POINT(".length())) {
            String coords = loc.replace("POINT(", "").replace(")", "").trim();
            String[] parts = coords.split("\\s+");
            if (parts.length < 2) return null;
            double lon = Double.parseDouble(parts[0]);
            double lat = Double.parseDouble(parts[1]);
            return new double[] { lon, lat };
        }

        // 2) EWKB hex: 0101000020E6100000{X}{Y} en little-endian
        // Exemple: 01 | 01000020 | E6100000 | <8 bytes X> | <8 bytes Y>
        if (loc.matches("(?i)^[0-9a-f]+$") && loc.length() >= 2 + 8 + 8 + 16 + 16) {
            byte[] bytes = hexToBytes(loc);

            ByteBuffer bb = ByteBuffer.wrap(bytes);
            byte endianFlag = bb.get(); // 0 = big, 1 = little
            bb.order(endianFlag == 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

            int type = bb.getInt(); // peut contenir le flag SRID (0x20000000)
            boolean hasSrid = (type & 0x20000000) != 0;
            int baseType = (type & 0xFF); // 1 = Point

            if (baseType != 1) return null;

            if (hasSrid) {
                bb.getInt(); // SRID (ex: 4326) -> on l'ignore
            }

            double x = bb.getDouble(); // longitude
            double y = bb.getDouble(); // latitude
            return new double[] { x, y };
        }

        return null;
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return out;
    }

    /**
     * Construit une location acceptée par ST_GeogFromText.
     * Ex: "SRID=4326;POINT(2.3522 48.8566)"
     */
    public void setLocationFromCoords(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) return;
        this.location = String.format(java.util.Locale.ROOT, "SRID=4326;POINT(%f %f)", longitude, latitude);
    }

    // Getters et Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
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
}
