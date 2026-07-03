# ============================================================
# CoupleApp Backend — Dockerfile (Render-ready)
# Two-stage build: compile with Maven, run with lean JRE image
# ============================================================

# Stage 1: Build the JAR using full Maven + JDK image
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first — Docker caches this layer separately.
# If only source code changed, Maven won't re-download all dependencies.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Now copy source and compile. -DskipTests speeds up build (run tests in CI pipeline)
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: Run with a minimal JRE image (~200MB vs ~600MB with full JDK)
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy only the compiled JAR — no source code or Maven in the production image
COPY --from=build /app/target/*.jar app.jar

# Render dynamically assigns a port via the $PORT environment variable
EXPOSE 8080

# Health check — Render polls this to confirm the app started successfully
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# -Xmx512m keeps memory within Render free tier limits
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
