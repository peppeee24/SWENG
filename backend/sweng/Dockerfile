FROM openjdk:17-jre-slim

# Installa curl per health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Crea directory applicazione
WORKDIR /app

# Copia il JAR
COPY target/nota-bene-backend-*.jar app.jar

# Espone porta 8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Comando di avvio
ENTRYPOINT ["java", "-jar", "app.jar"]
