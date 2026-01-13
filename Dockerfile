FROM maven:3.9.6-eclipse-temurin-17

WORKDIR /app

# On garde le cache Maven
COPY pom.xml .
RUN mvn dependency:go-offline

# Le code sera monté depuis Windows
CMD mvn spring-boot:run
