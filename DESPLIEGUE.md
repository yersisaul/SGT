# Despliegue de SGT con Docker

Tres contenedores orquestados con `docker-compose.yml`:

| Servicio   | Imagen                         | Función                                                        | Puerto publicado |
|------------|--------------------------------|----------------------------------------------------------------|------------------|
| `db`       | `postgres:16-alpine`           | Base de datos (volumen `pgdata`)                               | — (red interna)  |
| `backend`  | `sgt-backend:<APP_VERSION>`    | API Spring Boot (adjuntos en el volumen `uploads`)             | — (red interna)  |
| `frontend` | `sgt-frontend:<APP_VERSION>`   | nginx: sirve Angular y hace de proxy de `/api` hacia el backend | `HTTP_PORT` (80) |

Solo nginx queda expuesto. La red `data` (backend ↔ base de datos) no tiene salida a internet.

## Requisitos

- Docker Engine 24+ con Compose v2.
- ~2 GB de RAM libres para el build (la compilación de Angular es la etapa más exigente). En ejecución, el sistema completo usa ~1 GB.

> Los iconos viven en `src/frontend/src/app/shared/icons/` (solo los que usa SGT). No se usa el paquete
> `@lucide/angular` porque su build limpio exige más de 3,5 GB de RAM. Para agregar un icono, copia su `node`
> desde https://lucide.dev a `lucide-icon-data.ts` y declara su componente en `lucide-icons.ts`.

## Primer despliegue

1. Copia `.env.example` a `.env` y completa los valores. Obligatorios: `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (≥ 32 caracteres), `SEED_USERS_PASSWORD`.
2. Ajusta las variables de Docker:
   - `APP_VERSION`: versión que muestra la interfaz y etiqueta las imágenes (SemVer, p. ej. `1.0.0`).
   - `HTTP_PORT`: puerto del servidor donde se publica el sistema.
   - `CORS_ALLOWED_ORIGINS`: URL pública con la que se accede, **incluido el puerto si no es 80/443** (p. ej. `https://sgt.cfbd.co` o `http://localhost:81`). El frontend llama a la API en su mismo origen (`/api`, que nginx reenvía al backend), así que este valor solo aplica a clientes de otros orígenes.
   - `DB_NAME`, `NGINX_CLIENT_MAX_BODY_SIZE` (por encima de `FILE_MAX_SIZE_MB`).
3. Construye y levanta:

   ```bash
   docker compose up -d --build
   docker compose ps        # los tres servicios deben quedar "healthy"/"running"
   ```

En el primer arranque, Hibernate crea las tablas, `db/init.sql` crea las secuencias y el seeder carga el catálogo y los usuarios iniciales.

## Nueva versión

```bash
# 1. Actualiza APP_VERSION en .env (p. ej. 1.1.0)
git pull
docker compose up -d --build
```

Los volúmenes `pgdata` y `uploads` se conservan entre versiones.

## Verificación tras desplegar

```bash
curl -I http://localhost:${HTTP_PORT}/            # 200 y cabeceras de seguridad
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:${HTTP_PORT}/api/solicitudes   # 401 (protegido)
```

## Operación

```bash
docker compose logs -f backend                     # logs de la API
docker compose exec db pg_dump -U "$DB_USERNAME" -Fc sgt > sgt_$(date +%F).dump   # respaldo
docker compose exec -T db pg_restore -U "$DB_USERNAME" -d sgt --clean < sgt.dump  # restauración
docker compose down                                # detiene (conserva los datos)
```

`docker compose down -v` **borra los volúmenes** (base de datos y adjuntos): úsalo solo a propósito.

## HTTPS

nginx escucha en HTTP. En producción, coloca delante un terminador TLS (balanceador corporativo, Traefik, Caddy o nginx con certificado) que envíe `X-Forwarded-For`, `X-Forwarded-Proto` y `X-Forwarded-Port`, y declara sus redes en `NGINX_TRUSTED_PROXIES` (CIDR separados por espacio). Sin esa variable, nginx no confía en ninguna cabecera `X-Forwarded-*` y, detrás de un proxy:

- todos los usuarios comparten la IP del proxy, y por tanto **un único** límite de peticiones (`RATE_LIMIT_REQUESTS_PER_MINUTE`) y de intentos de login;
- Spring cree que la conexión es `http` y trata el `Origin: https://…` del navegador como petición CORS (403 si no está en `CORS_ALLOWED_ORIGINS`).

Solo se confía en esas cabeceras cuando la conexión llega desde las redes declaradas; un cliente que las envíe directamente no puede falsear su IP.

## Coolify

Usa `docker-compose.coolify.yml` (no `docker-compose.yml`): imágenes publicadas en Docker Hub, sin redes propias, sin `ports` y con `NGINX_TRUSTED_PROXIES` apuntando a las redes privadas de Docker.

1. Construye y publica las imágenes (el frontend **debe** incluir `proxies-confiables.sh`):

   ```bash
   docker build -t yersy78/sgt-backend:<versión> .
   docker build --build-arg APP_VERSION=<versión> -t yersy78/sgt-frontend:<versión> src/frontend
   docker push yersy78/sgt-backend:<versión> && docker push yersy78/sgt-frontend:<versión>
   ```

2. En el recurso de Coolify, reemplaza el compose por el contenido de `docker-compose.coolify.yml` y define en *Environment Variables* `APP_VERSION`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `SEED_USERS_PASSWORD` y el resto del `.env`.
3. Asigna el dominio **solo al servicio `frontend`**, con el puerto interno: `https://sgt.azorweb.dev:8080`. Backend y base de datos quedan sin dominio.
4. Redespliega el stack completo.

Por qué así:

| Síntoma detrás de Coolify | Causa | Corrección |
|---|---|---|
| `504 Gateway Timeout` intermitente | El contenedor estaba en varias redes (`web`, `data` y la del recurso); Traefik a veces elegía una que no alcanza | Sin redes propias: Coolify conecta todo a la red del recurso. Si se agregan redes, añadir al frontend la etiqueta `traefik.docker.network=<uuid-del-recurso>` |
| 429 y pantallas a medio cargar con varios usuarios | Rate limit compartido por la IP de Traefik | `NGINX_TRUSTED_PROXIES` |
| 403 "No tienes permiso" en POST/PUT/DELETE | Spring veía `http` y el navegador enviaba `Origin: https://…` | `NGINX_TRUSTED_PROXIES` (nginx reenvía el `X-Forwarded-Proto` de Traefik) |
| 502 tras redesplegar solo el backend | nginx guardaba la IP antigua del backend | `server backend:8080 resolve` en el `upstream` |

Las etiquetas `traefik.*` en el backend no se usan: `/api` entra por nginx, que ya tiene la configuración del SSE. Si se activaran, Traefik enviaría `/api` directo al backend, sin ella y con cabeceras CORS duplicadas.

Si publicas `ports` para entrar también por IP:puerto, acota `NGINX_TRUSTED_PROXIES` a la subred exacta de la red del recurso (`docker network inspect <uuid> --format '{{(index .IPAM.Config 0).Subnet}}'`); con los rangos privados completos, un equipo de la LAN podría falsear su IP ante el rate limit.

Si el dominio pasa por Cloudflare con el proxy activado (nube naranja), Traefik ve la IP de Cloudflare: configura en Traefik `forwardedHeaders.trustedIPs` con los rangos de Cloudflare.

## Notas de seguridad

- El backend usa `SERVER_FORWARD_HEADERS_STRATEGY=native` solo dentro de Docker: toma la IP real del cliente desde `X-Forwarded-For`, que nginx **sobrescribe** (no concatena) para que no se pueda falsear. Así el límite de peticiones y la protección anti fuerza bruta del login funcionan por usuario.
- Las imágenes corren con usuarios sin privilegios (`sgt` en el backend, `nginx` en el frontend).
- `.env` nunca entra en las imágenes: el build del backend solo copia `pom.xml` y `src/main`, y el `.env` se inyecta en tiempo de ejecución.
