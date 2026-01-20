Longitude Ouest : 47.48
Latitude Sud    : -18.98
Longitude Est   : 47.60
Latitude Nord   : -18.82


Je dois faire en sorte que les visiteurs voient une carte avec les différents points représentants les problèmes routiers, voici ce que j'ai fait :
1️⃣ Création du container PostGIS avec Docker
docker run -d \
  --name postgis \
  -p 5432:5432 \
  -e POSTGRES_DB=gisdb \
  -e POSTGRES_USER=gisuser \
  -e POSTGRES_PASSWORD=gispass \
  postgis/postgis:15-3.4
Crée un container Docker avec Postgres + PostGIS
Base par défaut : gisdb
Utilisateur : gisuser, mot de passe : gispass

2️⃣ Préparation des données OSM
Téléchargement du fichier OSM PBF pour Madagascar
Extraction de la zone d’Antananarivo avec osmium-tool :
osmium extract -b 47.48,-18.98,47.60,-18.82 madagascar-latest.osm.pbf -o antananarivo.osm.pbf

4️⃣ Creation de base gis_osm et extension hstore
docker exec -it postgis psql -U gisuser -c "CREATE DATABASE gis_osm;"
docker exec -it postgis psql -U gisuser -d gis_osm
CREATE EXTENSION IF NOT EXISTS hstore;
\q

5️⃣ Import OSM avec osm2pgsql
docker run --rm -it ^
  -v E:\Lysa\S5\Rojo\cloud\cloud\tiles:/data ^
  --network container:postgis ^
  iboates/osm2pgsql:latest ^
  -d gis_osm ^
  -U gisuser ^
  -H localhost ^
  -P 5432 ^
  --slim ^
  --hstore ^
  /data/antananarivo.osm.pbf


Tables créées :
planet_osm_point
planet_osm_line
planet_osm_polygon
planet_osm_roads
Index géométriques et osm_id ajoutés automatiquement.

Je dois maintenant afficher la carte (si possible sans python). 

