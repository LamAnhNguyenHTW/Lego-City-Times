# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Dependencies separat cachen – dieser Layer bleibt gültig solange pom.xml unverändert
RUN ./mvnw dependency:go-offline -q

COPY src src
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Nicht als root laufen — Upload-Verzeichnis vor dem User-Switch anlegen
RUN addgroup -S spring && adduser -S spring -G spring && \
    mkdir -p /app/uploads/images && \
    chown -R spring:spring /app

USER spring:spring

COPY --from=build /workspace/target/*.jar app.jar

# Upload-Verzeichnis als Volume-Einstiegspunkt (Bilder bleiben erhalten)
VOLUME /app/uploads

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

 