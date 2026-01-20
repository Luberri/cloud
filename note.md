Longitude Ouest : 47.48
Latitude Sud    : -18.98
Longitude Est   : 47.60
Latitude Nord   : -18.82

47.48,-18.98,47.60,-18.82


backend/Dockerfile :
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

frontend/Dockerfile :
FROM nginx:alpine
COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80

docker pull postgis/postgis
docker pull openjdk:17-jdk-slim
docker pull nginx:alpine
docker pull maptiler/tileserver-gl

docker run --rm -it `
  -v ${PWD}:/data `
  ghcr.io/osmcode/osmium-tool `
  osmium extract `
  -b 47.48,-18.98,47.60,-18.82 `
  /data/madagascar-latest.osm.pbf `
  -o /data/antananarivo.osm.pbf