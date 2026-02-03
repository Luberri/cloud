# Base : JDK 17
FROM eclipse-temurin:17-jdk

# Dossier de travail
WORKDIR /app

# Installer Maven si tu fais du spring-boot:run directement
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Copier le pom pour pré-charger les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline

# Le code sera monté depuis Windows
# Donc on n'a pas besoin de copier le src
CMD ["mvn", "spring-boot:run"]
