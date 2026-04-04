# ─── Stage 1: Build ───────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml first — lets Docker cache the dependency layer
# so re-builds only re-download deps when pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and package (skip tests — DB not available at build time)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─── Stage 2: Run ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S marketplace && adduser -S marketplace -G marketplace

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Give ownership to the non-root user
RUN chown marketplace:marketplace app.jar

USER marketplace

# Expose Spring Boot default port
EXPOSE 8080
ENV SERVER_ADDRESS=0.0.0.0

# Tuned JVM flags for containers:
#   -XX:+UseContainerSupport   → respect cgroup memory limits
#   -XX:MaxRAMPercentage=75.0  → use up to 75% of container RAM for heap
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
