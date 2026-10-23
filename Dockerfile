FROM node:22-alpine AS frontend
WORKDIR /build
COPY package.json package-lock.json ./
COPY frontend/package.json frontend/package.json
RUN npm ci --no-audit --no-fund
COPY frontend frontend
RUN npm run build

FROM maven:3.9-eclipse-temurin-25 AS backend
WORKDIR /build
COPY backend/pom.xml .
COPY backend/src src
COPY --from=frontend /build/frontend/dist src/main/resources/static
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system lootradar && useradd --system --gid lootradar lootradar
COPY --from=backend --chown=lootradar:lootradar /build/target/lootradar-0.1.0.jar app.jar
USER lootradar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
