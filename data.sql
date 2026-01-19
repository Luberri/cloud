-- UTILISATEURS BLOQUES POUR LES TESTS

-- bloqué depuis un moment
INSERT INTO users (email, password_hash, full_name, role_id, is_active,
                   failed_login_attempts, locked_until)
VALUES (
    'blocked1@example.com',
    'pwd123',
    'Utilisateur bloqué 1',
    (SELECT id FROM roles WHERE code = 'USER'),
    TRUE,
    3,
    NOW() - INTERVAL '1 day'
);

-- bloqué pour longtemps
INSERT INTO users (email, password_hash, full_name, role_id, is_active,
                   failed_login_attempts, locked_until)
VALUES (
    'blocked2@example.com',
    'pwd456',
    'Utilisateur bloqué 2',
    (SELECT id FROM roles WHERE code = 'USER'),
    TRUE,
    5,
    NOW() + INTERVAL '365 days'
);


-- Quelques signalements de test

INSERT INTO road_issues (
  title, description, location,
  surface_m2, budget, status_id, reported_by
)
VALUES
(
  'Nid de poule important',
  'Gros trou sur la chaussée, danger pour les vélos.',
  ST_SetSRID(ST_MakePoint(2.3522, 48.8566), 4326),
  5.50,
  8000.00,
  (SELECT id FROM road_issue_status WHERE code = 'NEW'),
  (SELECT id FROM users WHERE email = 'manager@admin.com')
),
(
  'Fissures sur la route',
  'Fissures longitudinales sur 30m, risque de dégradation rapide.',
  ST_SetSRID(ST_MakePoint(2.2945, 48.8584), 4326),
  30.00,
  25000.00,
  (SELECT id FROM road_issue_status WHERE code = 'IN_PROGRESS'),
  (SELECT id FROM users WHERE email = 'manager@admin.com')
),
(
  'Affaissement de chaussée',
  'Affaissement près d’un arrêt de bus, travaux urgents nécessaires.',
  ST_SetSRID(ST_MakePoint(2.3333, 48.8600), 4326),
  12.00,
  40000.00,
  (SELECT id FROM road_issue_status WHERE code = 'DONE'),
  (SELECT id FROM users WHERE email = 'manager@admin.com')
);