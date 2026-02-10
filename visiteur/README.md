# Cloud S5 - Gestion des Signalements Routiers

## Lancer le projet

### 1. Base de donnees + TileServer (Docker)

```bash
docker-compose up -d --build
```

### 2. Backend Spring Boot

```bash
cd web/backend
./mvnw spring-boot:run
```

Le backend demarre sur **http://localhost:8082**

### 3. Frontend React

```bash
cd web/frontend
npm install
npm run dev
```

Le frontend demarre sur **http://localhost:5174**

## Connexion

- **Email** : `manager@admin.com`
- **Mot de passe** : `hash123`