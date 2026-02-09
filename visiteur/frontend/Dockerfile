# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
# Replace hardcoded localhost URLs with relative paths for Docker deployment
RUN find src -name "*.ts" -o -name "*.tsx" | xargs sed -i 's|http://localhost:8082||g'
RUN sed -i 's|http://localhost:8081||g' src/pages/MapPage.tsx
RUN npm run build

# Runtime stage
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
RUN echo 'events {} \
http { \
    include /etc/nginx/mime.types; \
    server { \
        listen 80; \
        root /usr/share/nginx/html; \
        index index.html; \
        location / { \
            try_files $uri /index.html; \
        } \
        location /auth { \
            proxy_pass http://backend:8080; \
            proxy_set_header Host $host; \
            proxy_set_header X-Real-IP $remote_addr; \
        } \
        location /api { \
            proxy_pass http://backend:8080; \
            proxy_set_header Host $host; \
            proxy_set_header X-Real-IP $remote_addr; \
        } \
        location /road-issues { \
            proxy_pass http://backend:8080; \
            proxy_set_header Host $host; \
            proxy_set_header X-Real-IP $remote_addr; \
        } \
        location /public { \
            proxy_pass http://backend:8080; \
            proxy_set_header Host $host; \
            proxy_set_header X-Real-IP $remote_addr; \
        } \
        location /swagger-ui { \
            proxy_pass http://backend:8080; \
            proxy_set_header Host $host; \
        } \
        location /v3/api-docs { \
            proxy_pass http://backend:8080; \
            proxy_set_header Host $host; \
        } \
        location /styles { \
            proxy_pass http://tileserver:8080; \
            proxy_set_header Host $host; \
        } \
    } \
}' > /etc/nginx/nginx.conf
EXPOSE 80