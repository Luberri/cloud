-- ============================================================
-- PROJET CLOUD S5 - BASE DE DONNÉES UNIQUE
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
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) UNIQUE NOT NULL, -- VISITOR, USER, MANAGER
    description TEXT
);

INSERT INTO roles (code, description) VALUES
('USER', 'Utilisateur avec compte'),
('MANAGER', 'Gestionnaire');

-- =====================
-- UTILISATEURS (AUTH LOCALE)
-- =====================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    full_name VARCHAR(150),
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
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    token TEXT UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================
-- STATUTS DES SIGNALEMENTS
-- =====================
CREATE TABLE road_issue_status (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) UNIQUE NOT NULL, -- NEW, IN_PROGRESS, DONE
    label VARCHAR(100)
);

INSERT INTO road_issue_status (code, label) VALUES
('NEW', 'Nouveau'),
('IN_PROGRESS', 'En cours'),
('DONE', 'Terminé');

-- =====================
-- ENTREPRISES
-- =====================
CREATE TABLE companies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    contact_info TEXT
);

-- =====================
-- SIGNALEMENTS ROUTIERS
-- =====================
CREATE TABLE road_issues (
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

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================
-- JOURNAL DE SYNCHRONISATION
-- =====================
CREATE TABLE sync_logs (
    id SERIAL PRIMARY KEY,
    sync_type VARCHAR(50), -- PUSH / PULL
    entity VARCHAR(50), -- road_issue, user
    synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30),
    message TEXT
);

-- =====================
-- INDEXES (PERFORMANCE)
-- =====================
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_sessions_token ON user_sessions(token);
CREATE INDEX idx_road_issues_location ON road_issues USING GIST(location);
CREATE INDEX idx_road_issues_status ON road_issues(status_id);

-- =====================
-- VUE DE RÉCAPITULATION (VISITEUR)
-- =====================
CREATE VIEW v_road_issues_summary AS
SELECT
    COUNT(*) AS total_signalements,
    COALESCE(SUM(surface_m2), 0) AS total_surface_m2,
    COALESCE(SUM(budget), 0) AS total_budget,
    ROUND(
        100.0 * SUM(CASE WHEN s.code = 'DONE' THEN 1 ELSE 0 END)
        / NULLIF(COUNT(*), 0),
        2
    ) AS progress_percent
FROM road_issues r
JOIN road_issue_status s ON s.id = r.status_id;

-- =====================
-- MANAGER PAR DÉFAUT
-- =====================
INSERT INTO users (email, password_hash, full_name, role_id)
VALUES (
    'manager@admin.com',
    'hash123',
    'Gestionnaire',
    (SELECT id FROM roles WHERE code = 'MANAGER')
);

INSERT INTO users (email, password_hash, full_name, role_id)
VALUES (
    'manager@admin1.com',
    'hash123',
    'Gestionnaire',
    (SELECT id FROM roles WHERE code = 'MANAGER')
);
-- ============================================================
-- FIN DU SCRIPT
-- ============================================================





CREATE TABLE IF NOT EXISTS road_issue_status_history (
    id SERIAL PRIMARY KEY,
    road_issue_id UUID NOT NULL REFERENCES road_issues(id) ON DELETE CASCADE,
    status_id INT NOT NULL REFERENCES road_issue_status(id),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by UUID NULL REFERENCES users(id) -- manager qui change l'état (optionnel)
);

CREATE INDEX IF NOT EXISTS idx_rish_issue ON road_issue_status_history(road_issue_id);
CREATE INDEX IF NOT EXISTS idx_rish_status ON road_issue_status_history(status_id);
CREATE INDEX IF NOT EXISTS idx_rish_changed_at ON road_issue_status_history(changed_at);

CREATE TABLE IF NOT EXISTS image_issues (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    road_issue_id UUID NOT NULL REFERENCES road_issues(id) ON DELETE CASCADE,

    -- Chemin dans Firebase Storage (ex: road_issues/{firebase_id}/{image}.jpg)
    storage_path TEXT NOT NULL,

    -- (optionnel) URL directe / miniature (souvent générée côté backend via URL signée)
    download_url TEXT,
    thumbnail_url TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- éviter doublons lors des sync
CREATE UNIQUE INDEX IF NOT EXISTS uq_image_issues_issue_path
ON image_issues(road_issue_id, storage_path);

CREATE INDEX IF NOT EXISTS idx_image_issues_issue
ON image_issues(road_issue_id);


-- =====================
-- DONNÉES DE TEST : HISTORIQUE STATUTS POUR 1 ISSUE
-- =====================

INSERT INTO road_issue_status_history (
    road_issue_id,
    status_id,
    changed_at,
    changed_by
)
VALUES
-- 1) Nouveau
(
    '6314f1a6-2c09-49b3-a298-ec96ca6d39a0',
    (SELECT id FROM road_issue_status WHERE code = 'NEW'),
    '2026-02-03 02:55:07',
    (SELECT id FROM users WHERE email = 'manager@admin.com')
);
-- 2) En cours
INSERT INTO road_issue_status_history (
    road_issue_id,
    status_id,
    changed_at,
    changed_by
)
VALUES
(
    '6314f1a6-2c09-49b3-a298-ec96ca6d39a0',
    (SELECT id FROM road_issue_status WHERE code = 'IN_PROGRESS'),
    '2026-02-04 10:00:00',
    (SELECT id FROM users WHERE email = 'manager@admin.com')
),
-- 3) Terminé
(
    '6314f1a6-2c09-49b3-a298-ec96ca6d39a0',
    (SELECT id FROM road_issue_status WHERE code = 'DONE'),
    '2026-02-06 15:30:00',
    (SELECT id FROM users WHERE email = 'manager@admin1.com')
);