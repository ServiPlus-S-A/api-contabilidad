# ==============================================================================
# Stage 1 — Builder (Maven + JDK 25)
# ==============================================================================
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /build

# Download dependencies separately for Docker layer caching
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -q

# Build application (skip tests — tests run in CI, not in image build)
COPY src src
RUN ./mvnw package -DskipTests -q

# ==============================================================================
# Stage 2 — Runtime (JRE 25 — minimal attack surface)
# ==============================================================================
FROM eclipse-temurin:25-jre-alpine AS runtime

# Non-root user for security
RUN addgroup -S contabilidad && adduser -S contabilidad-api -G contabilidad

WORKDIR /app

# Copy only the executable jar from builder
COPY --from=builder /build/target/*.jar app.jar

RUN chown contabilidad-api:contabilidad app.jar

USER contabilidad-api

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
