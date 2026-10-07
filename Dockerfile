# --- 1. Frontal Angular ---
FROM node:24-alpine AS frontend
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npx ng build

# --- 2. Backend Spring Boot, con el frontal dentro como recursos estáticos ---
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
COPY --from=frontend /web/dist/ ./src/main/resources/static/
# Los tests se pasan en el portátil (los de integración necesitan Docker/Podman)
RUN mvn -q -B package -DskipTests

# --- 3. Ejecución ---
FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=backend /src/target/regalos3d-*.jar app.jar
USER app
EXPOSE 8085
# Límite del contenedor 512 MB: heap fijo bajo para dejar sitio a metaspace e hilos
ENV JAVA_OPTS="-Xms128m -Xmx320m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Duser.timezone=Atlantic/Canary"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
