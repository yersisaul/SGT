# syntax=docker/dockerfile:1
# ──────────────────────────────────────────────────────────────────────────
# SGT — Frontend (Angular). Node compila; nginx sin privilegios sirve los
# archivos estáticos y hace de proxy inverso de /api hacia el backend.
# Todas las rutas son prerender o render en cliente (app.routes.server.ts),
# así que no se necesita Node en ejecución.
# ──────────────────────────────────────────────────────────────────────────

FROM node:24-slim AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

COPY . .
# Versión visible en la interfaz (APP_VERSION del .env vía docker compose).
ARG APP_VERSION=0.0.0-dev
ENV APP_VERSION=${APP_VERSION}
# Acota la memoria del build (el CLI abre un worker por CPU). Con los iconos
# locales (shared/icons) el pico ronda 1 GB; ver DESPLIEGUE.md.
ENV NG_BUILD_MAX_WORKERS=2 \
    NODE_OPTIONS=--max-old-space-size=1536
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.29-alpine

# La imagen procesa /etc/nginx/templates/*.template con envsubst al arrancar.
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY nginx/security-headers.conf /etc/nginx/snippets/security-headers.conf
# Corre antes que el envsubst de las plantillas (20-*). Ver el script.
COPY --chmod=755 nginx/proxies-confiables.sh /docker-entrypoint.d/15-proxies-confiables.sh
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

ENV NGINX_CLIENT_MAX_BODY_SIZE=25m \
    NGINX_TRUSTED_PROXIES=""
EXPOSE 8080
