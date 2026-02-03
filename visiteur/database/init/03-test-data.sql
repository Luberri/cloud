-- ============================================================
-- DONNÉES DE TEST - UTILISATEURS (MOITIÉ BLOQUÉS, MOITIÉ ACTIFS)
-- ============================================================

-- Utilisateurs ACTIFS (non bloqués)
INSERT INTO users (email, password_hash, full_name, role_id, is_active, failed_login_attempts, locked_until)
VALUES
('jean.rakoto@gmail.com', 'pass123', 'Jean Rakotomalala', (SELECT id FROM roles WHERE code = 'USER'), TRUE, 0, NULL),
('marie.rabe@yahoo.fr', 'pass123', 'Marie Rabemananjara', (SELECT id FROM roles WHERE code = 'USER'), TRUE, 1, NULL),
('hery.andria@outlook.com', 'pass123', 'Hery Andriamanantena', (SELECT id FROM roles WHERE code = 'USER'), TRUE, 0, NULL),
('nomena.raza@gmail.com', 'pass123', 'Nomena Razafindrakoto', (SELECT id FROM roles WHERE code = 'USER'), TRUE, 2, NULL),
('tiana.rado@hotmail.com', 'pass123', 'Tiana Radonantenaina', (SELECT id FROM roles WHERE code = 'USER'), TRUE, 0, NULL),
('vonjy.randriam@gmail.com', 'pass123', 'Vonjy Randriamampionona', (SELECT id FROM roles WHERE code = 'USER'), TRUE, 1, NULL),
('faly.rasoa@yahoo.fr', 'pass123', 'Faly Rasoamanana', (SELECT id FROM roles WHERE code = 'USER'), TRUE, 0, NULL),
('aina.ratsima@gmail.com', 'pass123', 'Aina Ratsimamanga', (SELECT id FROM roles WHERE code = 'USER'), TRUE, 0, NULL),
('lova.raharison@outlook.com', 'pass123', 'Lova Raharison', (SELECT id FROM roles WHERE code = 'USER'), TRUE, 1, NULL)
ON CONFLICT (email) DO NOTHING;

-- Utilisateurs BLOQUÉS
INSERT INTO users (email, password_hash, full_name, role_id, is_active, failed_login_attempts, locked_until)
VALUES
('paul.ramana@gmail.com', 'pass123', 'Paul Ramanantsoavina', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 5, NOW() + INTERVAL '3 days'),
('sandra.ravo@yahoo.fr', 'pass123', 'Sandra Ravoniarisoa', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 4, NOW() + INTERVAL '7 days'),
('kevin.rakot@hotmail.com', 'pass123', 'Kevin Rakotonirina', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 6, NOW() + INTERVAL '2 days'),
('nirina.anja@gmail.com', 'pass123', 'Nirina Anjarasoa', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 3, NOW() + INTERVAL '5 days'),
('henintsoa.tojo@outlook.com', 'pass123', 'Henintsoa Tojoniaina', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 7, NOW() + INTERVAL '10 days'),
('miora.landy@gmail.com', 'pass123', 'Miora Landy', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 4, NOW() + INTERVAL '4 days'),
('todisoa.faniry@yahoo.fr', 'pass123', 'Todisoa Faniry', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 5, NOW() + INTERVAL '6 days'),
('mahery.solo@gmail.com', 'pass123', 'Mahery Solo', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 8, NOW() + INTERVAL '14 days'),
('irina.hasina@hotmail.com', 'pass123', 'Irina Hasina', (SELECT id FROM roles WHERE code = 'USER'), FALSE, 3, NOW() + INTERVAL '1 day')
ON CONFLICT (email) DO NOTHING;

-- ============================================================
-- FIN DES DONNÉES DE TEST
-- ============================================================
