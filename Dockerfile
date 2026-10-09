# syntax=docker/dockerfile:1
# ──────────────────────────────────────────────────────────────────────────
# SGT — Backend (Spring Boot). Build multi-etapa: Maven compila, la imagen
# final solo lleva el JRE y el jar, y corre como usuario sin privilegios.
# ──────────────────────────────────────────────────────────────────────────

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencias primero: esta capa se reutiliza mientras pom.xml no cambie.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src/main src/main
# Las pruebas requieren una BD y corren en CI/local (./mvnw test), no aquí.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package -Dmaven.test.skip=true \
 && cp target/*.jar /build/app.jar

FROM eclipse-temurin:21-jre

RUN groupadd --system sgt \
 && useradd --system --gid sgt --home-dir /app --shell /usr/sbin/nologin sgt \
 && mkdir -p /app/uploads \
 && chown -R sgt:sgt /app

WORKDIR /app
COPY --from=build --chown=sgt:sgt /build/app.jar app.jar

# La memoria del heap se adapta al límite del contenedor.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    FILE_STORAGE_PATH=/app/uploads

USER sgt
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
