-- ============================================================
-- DONNÉES INITIALES - UTILISATEUR MANAGER PAR DÉFAUT
-- ============================================================

-- MANAGER PAR DÉFAUT
INSERT INTO users (email, password_hash, full_name, role_id)
VALUES (
    'manager@admin.com',
    'hash123',
    'Gestionnaire',
    (SELECT id FROM roles WHERE code = 'MANAGER')
)
ON CONFLICT (email) DO NOTHING;

-- ============================================================
-- DONNÉES DE TEST - ENTREPRISES
-- ============================================================

INSERT INTO companies (name, contact_info) VALUES
('Colas Madagascar', 'contact@colas.mg, +261 20 22 123 45'),
('Sogea Satom', 'info@sogea.mg, +261 20 22 678 90'),
('Entreprise Razafindrakoto', 'razaf@gmail.com, +261 34 00 00 00')
ON CONFLICT DO NOTHING;

-- ============================================================
-- DONNÉES DE TEST - SIGNALEMENTS ROUTIERS
-- ============================================================

-- Signalements routiers (10 problèmes avec différents statuts)
INSERT INTO road_issues (title, description, location, surface_m2, budget, status_id, company_id, reported_by) VALUES
-- 3 problèmes terminés (DONE)
('Nid de poule RN7 km 12', 'Grand nid de poule dangereux', ST_GeographyFromText('POINT(47.5162 -18.9100)'), 15.50, 2500000, (SELECT id FROM road_issue_status WHERE code = 'DONE'), 1, (SELECT id FROM users WHERE email = 'manager@admin.com')),
('Fissure Avenue Indépendance', 'Fissure longitudinale de 50m', ST_GeographyFromText('POINT(47.5255 -18.9137)'), 75.00, 8500000, (SELECT id FROM road_issue_status WHERE code = 'DONE'), 2, (SELECT id FROM users WHERE email = 'manager@admin.com')),
('Affaissement Route Digue', 'Affaissement du sol sur 20m²', ST_GeographyFromText('POINT(47.5300 -18.9200)'), 20.00, 12000000, (SELECT id FROM road_issue_status WHERE code = 'DONE'), 1, (SELECT id FROM users WHERE email = 'manager@admin.com')),

-- 4 problèmes en cours (IN_PROGRESS)
('Dégradation RN1 Anosizato', 'Route très dégradée sur 100m', ST_GeographyFromText('POINT(47.4800 -18.9400)'), 200.00, 45000000, (SELECT id FROM road_issue_status WHERE code = 'IN_PROGRESS'), 2, (SELECT id FROM users WHERE email = 'manager@admin.com')),
('Nids de poule Analakely', 'Plusieurs nids de poule', ST_GeographyFromText('POINT(47.5200 -18.9100)'), 8.50, 1200000, (SELECT id FROM road_issue_status WHERE code = 'IN_PROGRESS'), 3, (SELECT id FROM users WHERE email = 'manager@admin.com')),
('Erosion bord de route Ivato', 'Erosion menaçant la chaussée', ST_GeographyFromText('POINT(47.4700 -18.8000)'), 45.00, 18000000, (SELECT id FROM road_issue_status WHERE code = 'IN_PROGRESS'), 1, (SELECT id FROM users WHERE email = 'manager@admin.com')),
('Fissures multiples Ambohijatovo', 'Réseau de fissures', ST_GeographyFromText('POINT(47.5350 -18.9180)'), 120.00, 15000000, (SELECT id FROM road_issue_status WHERE code = 'IN_PROGRESS'), 2, (SELECT id FROM users WHERE email = 'manager@admin.com')),

-- 3 problèmes nouveaux (NEW)
('Effondrement partiel Ambanidia', 'Début effondrement route', ST_GeographyFromText('POINT(47.5400 -18.9300)'), 35.00, 25000000, (SELECT id FROM road_issue_status WHERE code = 'NEW'), NULL, (SELECT id FROM users WHERE email = 'manager@admin.com')),
('Nid de poule géant Ankorondrano', 'Très grand nid de poule', ST_GeographyFromText('POINT(47.5100 -18.8900)'), 5.00, 800000, (SELECT id FROM road_issue_status WHERE code = 'NEW'), NULL, (SELECT id FROM users WHERE email = 'manager@admin.com')),
('Revêtement décollé Andraharo', 'Bitume décollé sur 30m²', ST_GeographyFromText('POINT(47.5050 -18.8850)'), 30.00, 9500000, (SELECT id FROM road_issue_status WHERE code = 'NEW'), NULL, (SELECT id FROM users WHERE email = 'manager@admin.com'))
ON CONFLICT DO NOTHING;

-- ============================================================
-- DONNÉES INITIALES - HISTORIQUE DES STATUTS
-- ============================================================

-- Insérer un historique initial pour chaque signalement existant
INSERT INTO road_issue_status_history (road_issue_id, status_id, changed_at, changed_by)
SELECT ri.id, ri.status_id, ri.reported_at, ri.reported_by
FROM road_issues ri
WHERE NOT EXISTS (
    SELECT 1 FROM road_issue_status_history h WHERE h.road_issue_id = ri.id
);

-- Ajouter des transitions pour les signalements EN COURS (NEW -> IN_PROGRESS)
INSERT INTO road_issue_status_history (road_issue_id, status_id, changed_at, changed_by)
SELECT ri.id,
       (SELECT id FROM road_issue_status WHERE code = 'NEW'),
       ri.reported_at - INTERVAL '5 days',
       ri.reported_by
FROM road_issues ri
JOIN road_issue_status s ON s.id = ri.status_id
WHERE s.code = 'IN_PROGRESS'
AND NOT EXISTS (
    SELECT 1 FROM road_issue_status_history h
    WHERE h.road_issue_id = ri.id
    AND h.status_id = (SELECT id FROM road_issue_status WHERE code = 'NEW')
);

-- Ajouter des transitions pour les signalements TERMINÉS (NEW -> IN_PROGRESS -> DONE)
INSERT INTO road_issue_status_history (road_issue_id, status_id, changed_at, changed_by)
SELECT ri.id,
       (SELECT id FROM road_issue_status WHERE code = 'NEW'),
       ri.reported_at - INTERVAL '10 days',
       ri.reported_by
FROM road_issues ri
JOIN road_issue_status s ON s.id = ri.status_id
WHERE s.code = 'DONE'
AND NOT EXISTS (
    SELECT 1 FROM road_issue_status_history h
    WHERE h.road_issue_id = ri.id
    AND h.status_id = (SELECT id FROM road_issue_status WHERE code = 'NEW')
);

INSERT INTO road_issue_status_history (road_issue_id, status_id, changed_at, changed_by)
SELECT ri.id,
       (SELECT id FROM road_issue_status WHERE code = 'IN_PROGRESS'),
       ri.reported_at - INTERVAL '5 days',
       ri.reported_by
FROM road_issues ri
JOIN road_issue_status s ON s.id = ri.status_id
WHERE s.code = 'DONE'
AND NOT EXISTS (
    SELECT 1 FROM road_issue_status_history h
    WHERE h.road_issue_id = ri.id
    AND h.status_id = (SELECT id FROM road_issue_status WHERE code = 'IN_PROGRESS')
);

-- ============================================================
-- FIN DES DONNÉES INITIALES
-- ============================================================
