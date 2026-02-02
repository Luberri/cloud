-- ============================================================
-- DONNÉES DE TEST - UTILISATEURS BLOQUÉS
-- ============================================================

-- Utilisateurs bloqués pour tests
INSERT INTO users (email, password_hash, full_name, role_id, is_active, failed_login_attempts, locked_until)
VALUES
('blocked4@example.com', 'hash_blocked4', 'Utilisateur Bloqué 4', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 7, NOW() + INTERVAL '4 days'),
('blocked5@example.com', 'hash_blocked5', 'Utilisateur Bloqué 5', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 8, NOW() + INTERVAL '5 days'),
('blocked6@example.com', 'hash_blocked6', 'Utilisateur Bloqué 6', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 6, NOW() + INTERVAL '6 hours')
ON CONFLICT (email) DO NOTHING;

-- Utilisateurs bloqués 7 à 20
INSERT INTO users (email, password_hash, full_name, role_id, is_active, failed_login_attempts, locked_until)
VALUES
('blocked7@example.com',  'hash_blocked7',  'Utilisateur Bloqué 7',  (SELECT id FROM roles WHERE code = 'USER'), FALSE, 4,  NOW() + INTERVAL '7 days'),
('blocked8@example.com',  'hash_blocked8',  'Utilisateur Bloqué 8',  (SELECT id FROM roles WHERE code = 'USER'), FALSE, 5,  NOW() + INTERVAL '8 days'),
('blocked9@example.com',  'hash_blocked9',  'Utilisateur Bloqué 9',  (SELECT id FROM roles WHERE code = 'USER'), FALSE, 6,  NOW() + INTERVAL '9 days'),
('blocked10@example.com', 'hash_blocked10', 'Utilisateur Bloqué 10', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 7,  NOW() + INTERVAL '10 days'),
('blocked11@example.com', 'hash_blocked11', 'Utilisateur Bloqué 11', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 8,  NOW() + INTERVAL '11 days'),
('blocked12@example.com', 'hash_blocked12', 'Utilisateur Bloqué 12', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 9,  NOW() + INTERVAL '12 days'),
('blocked13@example.com', 'hash_blocked13', 'Utilisateur Bloqué 13', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 10, NOW() + INTERVAL '13 days'),
('blocked14@example.com', 'hash_blocked14', 'Utilisateur Bloqué 14', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 11, NOW() + INTERVAL '14 days'),
('blocked15@example.com', 'hash_blocked15', 'Utilisateur Bloqué 15', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 12, NOW() + INTERVAL '15 days'),
('blocked16@example.com', 'hash_blocked16', 'Utilisateur Bloqué 16', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 13, NOW() + INTERVAL '16 days'),
('blocked17@example.com', 'hash_blocked17', 'Utilisateur Bloqué 17', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 14, NOW() + INTERVAL '17 days'),
('blocked18@example.com', 'hash_blocked18', 'Utilisateur Bloqué 18', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 15, NOW() + INTERVAL '18 days'),
('blocked19@example.com', 'hash_blocked19', 'Utilisateur Bloqué 19', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 16, NOW() + INTERVAL '19 days'),
('blocked20@example.com', 'hash_blocked20', 'Utilisateur Bloqué 20', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 17, NOW() + INTERVAL '20 days')
ON CONFLICT (email) DO NOTHING;

-- ============================================================
-- FIN DES DONNÉES DE TEST
-- ============================================================
