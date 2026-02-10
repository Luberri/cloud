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
CREATE TABLE IF NOT EXISTS prix_forfaitaire (
    id SERIAL PRIMARY KEY,
    prix DECIMAL(10, 2) NOT NULL DEFAULT 50000.00,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);