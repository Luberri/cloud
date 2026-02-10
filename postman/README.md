# Collection Postman - Cloud S5

## Demarrage

1. `docker compose up -d`
2. `cd visiteur/backend && ./mvnw spring-boot:run`
3. Serveur : http://localhost:8082

## Login

```
POST http://localhost:8082/auth/login
Body (raw JSON) :
{
  "email": "manager@admin.com",
  "password": "hash123"
}
```

## APIs publiques (sans login)

```
GET http://localhost:8082/public/summary
GET http://localhost:8082/public/statistics
GET http://localhost:8082/public/road-issues
GET http://localhost:8082/public/road-issues/{issueId}/images
GET http://localhost:8082/public/road-issues/{issueId}/images/count
```

## APIs protegees (header : Authorization: Bearer <token>)

### Authentication
```
POST http://localhost:8082/auth/login
POST http://localhost:8082/auth/login/firebase
POST http://localhost:8082/auth/register
PUT  http://localhost:8082/auth/update
POST http://localhost:8082/auth/unlock/{email}
POST http://localhost:8082/auth/unlock/id/{uuid}
GET  http://localhost:8082/auth/blocked
```

### Signalements
```
GET  http://localhost:8082/issues
PUT  http://localhost:8082/issues/{id}
GET  http://localhost:8082/issues/{id}/history
GET  http://localhost:8082/issues/{id}/images
POST http://localhost:8082/issues/{id}/history?statusId=2
```

### Utilisateurs
```
GET http://localhost:8082/users
GET http://localhost:8082/users/blocked
```

### Synchronisation Firebase
```
POST http://localhost:8082/sync/all
POST http://localhost:8082/sync/status-changes
POST http://localhost:8082/sync/images
POST http://localhost:8082/sync/images/pull
POST http://localhost:8082/sync/images/push
```
