# Projet Cloud S5 - Application Unifiée

Ce projet combine les applications **Manager** et **Visiteur** en une seule application unifiée.

## Architecture

```
visiteur/
├── backend/          # API Spring Boot (JDK 17)
├── frontend/         # Application React/Vite/TypeScript
├── database/         # Scripts SQL
│   └── init/         # Scripts d'initialisation Docker
├── tiles/            # Données de carte OSM
└── compose.yaml      # Docker Compose (PostgreSQL + TileServer)
```

## Prérequis

- **JDK 17** (local)
- **Node.js 18+** (local)
- **Docker & Docker Compose**

## Démarrage

### 1. Lancer PostgreSQL avec Docker

```bash
cd visiteur
docker-compose up -d
```

Cela démarre :
- **PostgreSQL/PostGIS** sur le port `5433` avec la base `cloud_db`
- **TileServer** sur le port `8081` pour les cartes

Les scripts SQL dans `database/init/` sont exécutés automatiquement :
1. `01-schema.sql` - Création des tables
2. `02-data.sql` - Données initiales (manager, entreprises, signalements)
3. `03-test-data.sql` - Utilisateurs de test bloqués

### 2. Lancer le Backend Spring Boot

```bash
cd visiteur/backend
./mvnw spring-boot:run
```

Le backend démarre sur `http://localhost:8082`

### 3. Lancer le Frontend

```bash
cd visiteur/frontend
npm install
npm run dev
```

Le frontend démarre sur `http://localhost:5173`

## Accès à l'application

### Pages publiques (Visiteur)
- **Résumé** : `http://localhost:5173/` - Vue d'ensemble des signalements
- **Carte** : `http://localhost:5173/map` - Carte interactive des problèmes routiers

### Pages Manager (authentifié)
- **Connexion** : `http://localhost:5173/login` ou cliquer sur "Espace Manager"
- **Accueil Manager** : Dashboard après connexion
- Gestion des utilisateurs
- Gestion des signalements

## Compte Manager par défaut

```
Email: manager@admin.com
Mot de passe: hash123
```

## Base de données

La base PostgreSQL est accessible sur :
- **Host** : localhost
- **Port** : 5433
- **Database** : cloud_db
- **User** : postgres
- **Password** : postgres

## API Endpoints

### Auth
- `POST /auth/login` - Connexion
- `POST /auth/register` - Inscription
- `GET /auth/blocked` - Utilisateurs bloqués
- `POST /auth/unlock/id/{id}` - Débloquer un utilisateur

### Users
- `GET /users` - Liste des utilisateurs

### Issues
- `GET /issues` - Liste des signalements
- `PUT /issues/{id}` - Modifier un signalement

### Public
- `GET /api/public/summary` - Résumé public
- `GET /api/public/map/issues` - Points pour la carte

## Arrêter les services

```bash
# Arrêter Docker
docker-compose down

# Pour supprimer aussi les données
docker-compose down -v
```

## Notes techniques

- Le backend utilise Spring Boot 4.0.1 avec JPA/Hibernate
- Le frontend utilise React 18 + Vite + TypeScript
- Les cartes utilisent Leaflet avec TileServer-GL
- L'authentification est gérée par JWT
