# Collection Postman - Cloud S5

## Demarrage rapide

1. Demarrer les containers Docker : `docker compose up -d`
2. Demarrer le backend : `cd visiteur/backend && ./mvnw spring-boot:run`
3. Le serveur tourne sur **http://localhost:8082**

## Toutes les APIs (24 endpoints)

### Authentication (POST/PUT/GET - /auth)

| Methode | URL | Body | Description |
|---------|-----|------|-------------|
| POST | http://localhost:8082/auth/login | `{"email":"manager@admin.com","password":"hash123"}` | Login PostgreSQL |
| POST | http://localhost:8082/auth/login/firebase | `{"firebaseIdToken":"token"}` | Login Firebase |
| POST | http://localhost:8082/auth/register | `{"email":"user@example.com","password":"password123","fullName":"Jean Dupont","useFirebase":false}` | Inscription |
| PUT | http://localhost:8082/auth/update | `{"email":"admin@example.com","newEmail":"new@example.com","newFullName":"Nom","newPassword":"pass"}` | Modifier utilisateur |
| POST | http://localhost:8082/auth/unlock/admin@example.com | - | Debloquer par email |
| POST | http://localhost:8082/auth/unlock/id/{uuid} | - | Debloquer par ID |
| GET | http://localhost:8082/auth/blocked | - | Liste utilisateurs bloques |

### Public APIs (GET - /public) - Pas d'authentification

| Methode | URL | Description |
|---------|-----|-------------|
| GET | http://localhost:8082/public/summary | Resume des signalements |
| GET | http://localhost:8082/public/statistics | Statistiques |
| GET | http://localhost:8082/public/road-issues | Points signalements |
| GET | http://localhost:8082/public/road-issues/{issueId}/images | Images d'un signalement |
| GET | http://localhost:8082/public/road-issues/{issueId}/images/count | Nombre d'images |

### Signalements routiers (GET/PUT/POST - /issues) - Auth requise

| Methode | URL | Body | Description |
|---------|-----|------|-------------|
| GET | http://localhost:8082/issues | - | Lister tous les signalements |
| PUT | http://localhost:8082/issues/{id} | `{"title":"Nid de poule","description":"...","surfaceM2":5.0,"budget":1000,"statusId":2}` | Modifier un signalement |
| GET | http://localhost:8082/issues/{id}/history | - | Historique des statuts |
| GET | http://localhost:8082/issues/{id}/images | - | Images d'un signalement |
| POST | http://localhost:8082/issues/{id}/history?statusId=2 | - | Ajouter changement statut |

### Utilisateurs (GET - /users) - Auth requise

| Methode | URL | Description |
|---------|-----|-------------|
| GET | http://localhost:8082/users | Tous les utilisateurs |
| GET | http://localhost:8082/users/blocked | Utilisateurs bloques |

### Synchronisation Firebase (POST - /sync) - Auth requise

| Methode | URL | Description |
|---------|-----|-------------|
| POST | http://localhost:8082/sync/all | Synchronisation complete |
| POST | http://localhost:8082/sync/status-changes | Sync changements de statut |
| POST | http://localhost:8082/sync/images | Sync images bidirectionnelle |
| POST | http://localhost:8082/sync/images/pull | Pull images depuis Firebase |
| POST | http://localhost:8082/sync/images/push | Push images vers Firebase |

## Comment tester

### 1. APIs publiques (sans login)
Tester directement :
- http://localhost:8082/public/summary
- http://localhost:8082/public/statistics
- http://localhost:8082/public/road-issues

### 2. Login
POST http://localhost:8082/auth/login avec Body (raw JSON) :
```json
{
  "email": "manager@admin.com",
  "password": "hash123"
}
```
Copier le token JWT retourne.

### 3. APIs protegees
Ajouter le header : `Authorization: Bearer <token_jwt>` puis tester les endpoints /issues, /users, /sync.

## Fichiers

- `postman-collection.json` : Collection Postman avec les 24 requetes
- `README.md` : Ce fichier
