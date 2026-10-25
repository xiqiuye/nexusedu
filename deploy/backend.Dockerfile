FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/nexusedu-server/pom.xml backend/nexusedu-server/mvnw backend/nexusedu-server/mvnw.cmd backend/nexusedu-server/
COPY backend/nexusedu-server/.mvn backend/nexusedu-server/.mvn
RUN chmod +x backend/nexusedu-server/mvnw
RUN cd backend/nexusedu-server && ./mvnw -B -DskipTests dependency:go-offline
COPY backend/nexusedu-server/src backend/nexusedu-server/src
RUN cd backend/nexusedu-server && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 appuser \
    && mkdir -p /app/data/knowledge-uploads \
    && chown -R appuser:appuser /app
COPY --from=build /workspace/backend/nexusedu-server/target/nexusedu-server-0.0.1-SNAPSHOT.jar /app/app.jar
USER appuser
EXPOSE 9090
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
