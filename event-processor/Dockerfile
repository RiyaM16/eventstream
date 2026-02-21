# ── Stage 1: Build ────────────────────────────────────────────────────────────
# Use official Maven + JDK image - no need to install Maven ourselves
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

# Copy pom first - Docker caches this layer separately from source code
# Dependencies only re-download when pom.xml changes, not when source changes
COPY pom.xml ./
RUN mvn dependency:go-offline -q

# Now copy source and build
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
# JRE only - no compiler, no Maven, no source code in final image (~180MB vs ~400MB)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8081
# UseContainerSupport: JVM respects Docker memory limits instead of reading host RAM
# MaxRAMPercentage=75: use up to 75% of container memory for heap
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
