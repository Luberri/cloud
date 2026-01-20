import geopandas as gpd
from sqlalchemy import create_engine

# Connexion à PostgreSQL Docker
engine = create_engine('postgresql://gisuser:gispass@localhost:5432/gis_osm')

# Lire le GeoJSON généré par Osmium
gdf = gpd.read_file(r"E:\Lysa\S5\Rojo\cloud\cloud\tiles\antananarivo.json")

# Envoyer les données dans PostGIS
gdf.to_postgis("osm_roads", engine, if_exists="replace", index=False)

print("Import terminé !")
