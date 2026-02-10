-- Table road_issues avec colonne niveau
CREATE TABLE IF NOT EXISTS road_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255),
    description TEXT,
    location GEOGRAPHY(Point, 4326),
    surface_m2 DECIMAL(10, 2),
    budget DECIMAL(15, 2),
    status_id INTEGER,
    company_id INTEGER,
    reported_by UUID,
    reported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_synced BOOLEAN DEFAULT FALSE,
    firebase_id VARCHAR(255),
    updated_at TIMESTAMP,
    niveau INTEGER DEFAULT 1 CHECK (niveau >= 1 AND niveau <= 10),
    
    -- Contraintes
    CONSTRAINT fk_status FOREIGN KEY (status_id) REFERENCES road_issue_status(id),
    CONSTRAINT fk_company FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT check_surface_positive CHECK (surface_m2 >= 0),
    CONSTRAINT check_budget_positive CHECK (budget >= 0)
);

-- Ajouter la colonne niveau si la table existe déjà
ALTER TABLE road_issues ADD COLUMN IF NOT EXISTS niveau INTEGER DEFAULT 1 CHECK (niveau >= 1 AND niveau <= 10);

-- Index
CREATE INDEX IF NOT EXISTS idx_road_issues_niveau ON road_issues(niveau);
CREATE INDEX IF NOT EXISTS idx_road_issues_location ON road_issues USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_road_issues_status ON road_issues(status_id);

-- Vue avec détails
CREATE OR REPLACE VIEW v_road_issues_with_details AS
SELECT 
    ri.id,
    ri.title,
    ri.description,
    ST_Y(ri.location::geometry) AS latitude,
    ST_X(ri.location::geometry) AS longitude,
    ri.surface_m2,
    ri.budget,
    ri.niveau,
    CASE 
        WHEN ri.niveau BETWEEN 1 AND 3 THEN 'Faible'
        WHEN ri.niveau BETWEEN 4 AND 6 THEN 'Moyen'
        ELSE 'Critique'
    END AS niveau_label,
    ri.status_id,
    ris.label AS status_label,
    ris.code AS status_code,
    ri.company_id,
    c.name AS company_name,
    ri.reported_by,
    ri.reported_at,
    ri.updated_at
FROM road_issues ri
LEFT JOIN road_issue_status ris ON ri.status_id = ris.id
LEFT JOIN companies c ON ri.company_id = c.id;

-- Table prix forfaitaire
CREATE TABLE IF NOT EXISTS prix_forfaitaire (
    id SERIAL PRIMARY KEY,
    prix DECIMAL(10, 2) NOT NULL DEFAULT 50000.00,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insérer le prix par défaut
INSERT INTO prix_forfaitaire (prix, updated_at) 
VALUES (50000.00, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;