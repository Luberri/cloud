


-- Bloqué depuis un moment (devrait pouvoir être débloqué)
INSERT INTO users (
    email, password_hash, full_name, role_id,
    is_active, failed_login_attempts, locked_until
)
VALUES (
    'blocked1@example.com',
    'pwd123',
    'Utilisateur bloqué 1',
    (SELECT id FROM roles WHERE code = 'USER'),
    TRUE,
    3,
    NOW() - INTERVAL '1 day'
);

-- Bloqué pour longtemps
INSERT INTO users (
    email, password_hash, full_name, role_id,
    is_active, failed_login_attempts, locked_until
)
VALUES (
    'blocked2@example.com',
    'pwd456',
    'Utilisateur bloqué 2',
    (SELECT id FROM roles WHERE code = 'USER'),
    TRUE,
    5,
    NOW() + INTERVAL '365 days'
);

-- Nouveaux utilisateurs de test (emails uniques)
INSERT INTO users (
    email, password_hash, full_name, role_id,
    is_active, failed_login_attempts, locked_until
)
VALUES (
    'testuser1_2026@example.com',
    'pwd789',
    'Test User 1',
    (SELECT id FROM roles WHERE code = 'USER'),
    TRUE,
    0,
    NULL
),
(
    'testuser2_2026@example.com',
    'pwd101',
    'Test User 2',
    (SELECT id FROM roles WHERE code = 'USER'),
    TRUE,
    1,
    NOW() - INTERVAL '2 days'
);
