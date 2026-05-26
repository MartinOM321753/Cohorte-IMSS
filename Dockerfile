# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar solo el pom primero para cachear dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiar fuentes y compilar
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true -q && mv target/*.jar app.jar

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
