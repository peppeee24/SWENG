FROM eclipse-temurin:17-jre-alpine

# Installa curl per health checks
RUN apk update && apk add --no-cache curl

# Crea directory applicazione
WORKDIR /app

# Copia il jar
COPY target/backend.jar /app/app.jar

# Espone la porta
EXPOSE 8080

# Avvia l'applicazione
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
