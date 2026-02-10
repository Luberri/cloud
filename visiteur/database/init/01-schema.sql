-- ============================================================
-- PROJET CLOUD S5 - SCHÉMA DE BASE DE DONNÉES
-- PostgreSQL + PostGIS
-- ============================================================

-- =====================
-- EXTENSIONS
-- =====================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS postgis;

-- =====================
-- ROLES UTILISATEURS
-- =====================
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) UNIQUE NOT NULL, -- USER, MANAGER
    description TEXT
);

INSERT INTO roles (code, description) VALUES
('USER', 'Utilisateur avec compte'),
('MANAGER', 'Gestionnaire')
ON CONFLICT (code) DO NOTHING;

-- =====================
-- UTILISATEURS (AUTH LOCALE + FIREBASE)
-- =====================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    full_name VARCHAR(150),
    firebase_uid VARCHAR(150),
    role_id INT REFERENCES roles(id),
    is_active BOOLEAN DEFAULT TRUE,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================
-- SESSIONS UTILISATEURS
-- =====================
CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    token TEXT UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================
-- STATUTS DES SIGNALEMENTS
-- =====================
CREATE TABLE IF NOT EXISTS road_issue_status (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) UNIQUE NOT NULL, -- NEW, IN_PROGRESS, DONE
    label VARCHAR(100)
);

INSERT INTO road_issue_status (code, label) VALUES
('NEW', 'Nouveau'),
('IN_PROGRESS', 'En cours'),
('DONE', 'Terminé')
ON CONFLICT (code) DO NOTHING;

-- =====================
-- ENTREPRISES
-- =====================
CREATE TABLE IF NOT EXISTS companies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    contact_info TEXT
);

-- =====================
-- SIGNALEMENTS ROUTIERS
-- =====================
CREATE TABLE IF NOT EXISTS road_issues (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(150),
    description TEXT,
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    surface_m2 NUMERIC(10,2),
    budget NUMERIC(14,2),
    status_id INT REFERENCES road_issue_status(id),
    company_id INT REFERENCES companies(id),
    reported_by UUID REFERENCES users(id),
    reported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_synced BOOLEAN DEFAULT FALSE,
    firebase_id VARCHAR(150),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    niveau INTEGER DEFAULT 1 CHECK (niveau >= 1 AND niveau <= 10),
    
    CONSTRAINT check_surface_positive CHECK (surface_m2 >= 0),
    CONSTRAINT check_budget_positive CHECK (budget >= 0)
);

-- Ajouter la colonne niveau si la table existe déjà
ALTER TABLE road_issues ADD COLUMN IF NOT EXISTS niveau INTEGER DEFAULT 1;

-- Ajouter la contrainte CHECK si elle n'existe pas
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'check_niveau_range'
    ) THEN
        ALTER TABLE road_issues ADD CONSTRAINT check_niveau_range 
        CHECK (niveau >= 1 AND niveau <= 10);
    END IF;
END $$;

-- =====================
-- IMAGES DES SIGNALEMENTS
-- =====================
CREATE TABLE IF NOT EXISTS issue_images (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    road_issue_id UUID NOT NULL REFERENCES road_issues(id) ON DELETE CASCADE,
    storage_path TEXT NOT NULL,
    download_url TEXT NOT NULL,
    thumbnail_url TEXT,
    file_size_bytes BIGINT,
    mime_type VARCHAR(50) DEFAULT 'image/jpeg',
    uploaded_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_issue_images_road_issue ON issue_images(road_issue_id);

-- =====================
-- PRIX FORFAITAIRE 
-- =====================
CREATE TABLE IF NOT EXISTS prix_forfaitaire (
    id SERIAL PRIMARY KEY,
    prix NUMERIC(10, 2) NOT NULL DEFAULT 50000.00,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_prix_positive CHECK (prix > 0)
);

-- Insérer le prix par défaut
INSERT INTO prix_forfaitaire (prix, updated_at) 
VALUES (50000.00, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- =====================
-- JOURNAL DE SYNCHRONISATION
-- =====================
CREATE TABLE IF NOT EXISTS sync_logs (
    id SERIAL PRIMARY KEY,
    sync_type VARCHAR(50), -- PUSH / PULL
    entity VARCHAR(50), -- road_issue, user
    synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30),
    message TEXT
);

-- =====================
-- HISTORIQUE DES CHANGEMENTS DE STATUT
-- =====================
CREATE TABLE IF NOT EXISTS road_issue_status_history (
    id SERIAL PRIMARY KEY,
    road_issue_id UUID NOT NULL REFERENCES road_issues(id) ON DELETE CASCADE,
    status_id INT NOT NULL REFERENCES road_issue_status(id),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by UUID REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_status_history_issue ON road_issue_status_history(road_issue_id);
CREATE INDEX IF NOT EXISTS idx_status_history_status ON road_issue_status_history(status_id);

-- =====================
-- INDEXES (PERFORMANCE)
-- =====================
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_sessions_token ON user_sessions(token);
CREATE INDEX IF NOT EXISTS idx_road_issues_location ON road_issues USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_road_issues_status ON road_issues(status_id);
CREATE INDEX IF NOT EXISTS idx_road_issues_niveau ON road_issues(niveau);

-- =====================
-- VUE DE RÉCAPITULATION 
-- =====================
CREATE OR REPLACE VIEW v_road_issues_summary AS
SELECT
    COUNT(*) AS total_signalements,
    COALESCE(SUM(surface_m2), 0) AS total_surface_m2,
    COALESCE(SUM(budget), 0) AS total_budget,
    ROUND(
        100.0 * SUM(CASE WHEN s.code = 'DONE' THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0),
        2
    ) AS progress_percent,
    -- ✅ Statistiques par niveau
    COUNT(CASE WHEN r.niveau BETWEEN 1 AND 3 THEN 1 END) AS niveau_faible,
    COUNT(CASE WHEN r.niveau BETWEEN 4 AND 6 THEN 1 END) AS niveau_moyen,
    COUNT(CASE WHEN r.niveau BETWEEN 7 AND 10 THEN 1 END) AS niveau_critique
FROM road_issues r
JOIN road_issue_status s ON s.id = r.status_id;

-- =====================
-- VUE DÉTAILLÉE DES SIGNALEMENTS
-- =====================
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
        WHEN ri.niveau BETWEEN 7 AND 10 THEN 'Critique'
        ELSE 'Non défini'
    END AS niveau_label,
    ri.status_id,
    ris.label AS status_label,
    ris.code AS status_code,
    ri.company_id,
    c.name AS company_name,
    ri.reported_by,
    ri.reported_at,
    ri.updated_at,
    ri.is_synced,
    ri.firebase_id
FROM road_issues ri
LEFT JOIN road_issue_status ris ON ri.status_id = ris.id
LEFT JOIN companies c ON ri.company_id = c.id;

-- =====================
-- FONCTION POUR CALCULER LE BUDGET AUTOMATIQUEMENT 
-- =====================
CREATE OR REPLACE FUNCTION calculate_budget()
RETURNS TRIGGER AS $$
DECLARE
    prix_unitaire NUMERIC(10,2);
BEGIN
    -- Récupérer le prix forfaitaire actuel
    SELECT prix INTO prix_unitaire FROM prix_forfaitaire ORDER BY updated_at DESC LIMIT 1;
    
    -- Si surface_m2 est défini et budget n'est pas défini
    IF NEW.surface_m2 IS NOT NULL AND (NEW.budget IS NULL OR NEW.budget = 0) THEN
        NEW.budget := NEW.surface_m2 * COALESCE(prix_unitaire, 50000);
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Créer le trigger
DROP TRIGGER IF EXISTS trg_calculate_budget ON road_issues;
CREATE TRIGGER trg_calculate_budget
    BEFORE INSERT OR UPDATE OF surface_m2
    ON road_issues
    FOR EACH ROW
    EXECUTE FUNCTION calculate_budget();

-- =====================
-- STATISTIQUES PAR NIVEAU 
-- =====================
CREATE OR REPLACE VIEW v_road_issues_by_niveau AS
SELECT 
    niveau,
    CASE 
        WHEN niveau BETWEEN 1 AND 3 THEN 'Faible'
        WHEN niveau BETWEEN 4 AND 6 THEN 'Moyen'
        WHEN niveau BETWEEN 7 AND 10 THEN 'Critique'
        ELSE 'Non défini'
    END AS niveau_label,
    COUNT(*) as total_signalements,
    SUM(CASE WHEN status_id = 1 THEN 1 ELSE 0 END) as nouveau,
    SUM(CASE WHEN status_id = 2 THEN 1 ELSE 0 END) as en_cours,
    SUM(CASE WHEN status_id = 3 THEN 1 ELSE 0 END) as termine,
    COALESCE(SUM(surface_m2), 0) as total_surface_m2,
    COALESCE(SUM(budget), 0) as total_budget
FROM road_issues
GROUP BY niveau
ORDER BY niveau;

-- ============================================================
-- REQUÊTES UTILES POUR TESTS
-- ============================================================

-- Afficher les statistiques globales
-- SELECT * FROM v_road_issues_summary;

-- Afficher les signalements avec détails
-- SELECT * FROM v_road_issues_with_details;

-- Afficher les statistiques par niveau
-- SELECT * FROM v_road_issues_by_niveau;

-- Afficher le prix forfaitaire actuel
-- SELECT * FROM prix_forfaitaire ORDER BY updated_at DESC LIMIT 1;

-- ============================================================
-- FIN DU SCRIPT SCHEMA
-- ============================================================