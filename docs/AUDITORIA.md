# Auditoría general — SGT (backend)

Fecha: 2026-10-03 · Alcance: `src/main/java/cfbd/co/sgt` contrastado con CLAUDE.md §1 (flujo de 14 pasos) y §6 (seguridad).
Rutas relativas a `src/main/java/cfbd/co/sgt/`.

Severidad: 🔴 Crítico · 🟠 Alto · 🟡 Medio · ⚪ Bajo

---

## A. Brechas contra la lógica del negocio (§1)

| # | Paso | Hallazgo | Ubicación | Sev. |
|---|------|----------|-----------|------|
| N1 | ✅ **Resuelto (etapa 3): la OT se genera en la cola de la especialidad elegida, sin ejecutor; la toma un miembro o la asigna el responsable.** 5, 9 | **No existe autoasignación por especialidad.** El ejecutor de la OT lo elige a mano quien la genera (`id_usuario_ejecutor`) y solo se valida que tenga rol `Operaciones`. `Usuario` no tiene relación con `Especialidad`, por lo que hoy no hay dato para decidir “responsable por especialidad”. Requiere cambio de modelo (p. ej. `usuario.id_especialidad` o tabla `usuario_especialidad`) → **pedir aprobación (CLAUDE.md §3.3)**. | `service/impl/SolicitudServiceImpl.java:396`, `service/impl/RequerimientoServiceImpl.java:353`, `model/Usuario.java` | 🔴 |
| N2 | ✅ **Resuelto (etapa 3): `POST /ordenes/{id}/reasignar` cambia la especialidad (responsable o Admin) y deja la OT en la cola destino.** 12 | **La reasignación es entre personas, no entre especialidades.** `reasignarOrden` solo cambia `orden.usuario` (otro usuario Operaciones cualquiera) y **nunca cambia `orden.especialidad`**. Tampoco valida que el nuevo ejecutor pertenezca a la especialidad destino. | `service/impl/OrdenServiceImpl.java:212-250` | 🔴 |
| N3 | ✅ **Resuelto (etapa 3): `POST /ordenes/{id}/verificar` (corresponde → En progreso; no → Devuelta al responsable).** 11 | No hay paso explícito “verificar si corresponde” (aceptar/devolver OT). La OT pasa de `Pendiente` a `En progreso` vía PUT genérico sin registrar la decisión. Sugerido: operación `POST /ordenes/{id}/aceptar` o usar `reasignar` como única salida del “NO”. | `service/impl/OrdenServiceImpl.java:106` | 🟡 |
| N4 | ✅ **Resuelto (etapa 3): rechazar el RQ pasa la Solicitud de origen a "Rechazado" con el motivo en su historial.** 8 | **RQ rechazado deja la Solicitud de origen huérfana en “En revisión” para siempre.** `crearAprobacion(aprobado=false)` pasa el RQ a `Rechazado` pero no toca `requerimiento.solicitud`. El cliente nunca ve el cierre (paso 2) y el flujo dice “NO → FIN”. | `service/impl/AprobacionServiceImpl.java:66-110` | 🟠 |
| N5 | ✅ **Resuelto (etapa 3): aprobar con `id_especialidad_orden` genera la OT en la misma transacción; sin ella queda "Aprobado" para generación manual.** 9 | Aprobar no genera la OT: es un segundo paso manual (`requerimiento.generar_orden`, solo Administrador). Un RQ puede quedar `Aprobado` indefinidamente sin OT. Decidir si debe ser automático al aprobar. | `service/impl/RequerimientoServiceImpl.java:186` | 🟡 |
| N6 | ✅ **Resuelto (etapa 1): tras despachar no se cambian activo ni especialidad (409).** 3 | Despachador “clasifica” editando la Solicitud con el PUT genérico, que permite cambiar `especialidad`/`activo` **incluso después de generada la OT/RQ** (no hay guarda por estado). | `service/impl/SolicitudServiceImpl.java:132-148` | 🟡 |
| N7 | ✅ **Resuelto (etapa 1): toda Solicitud nace "Pendiente"; su especialidad se toma del activo.** 1 | `crearSolicitud` acepta `id_estado` del cliente: un Cliente puede crear una Solicitud directamente en `Finalizado`/`En progreso` y saltarse al Despachador. El estado inicial debe forzarse a `Pendiente`. | `service/impl/SolicitudServiceImpl.java:116` | 🟠 |
| N8 | — | `Derivacion` (Solicitud → usuario) no participa del flujo: no cambia estado, no valida rol destino y no tiene equivalente para OT. Es concepto heredado; el paso 12 lo cubre `orden.reasignar`. Decidir si se elimina o se reorienta. | `service/impl/DerivacionServiceImpl.java` | ⚪ |
| N9 | 6 | `crearRequerimiento` independiente (sin Solicitud) sigue habilitado para Despachador **y Operaciones**; el flujo solo contempla RQ nacido de una Solicitud fuera de contrato. | `config/DataSeeder.java` (matriz), `service/impl/RequerimientoServiceImpl.java:105` | 🟡 |
| N10 | — | Tabla de transiciones de RQ contempla `pendiente ↔ en revisión`, pero ningún RQ nace en `Pendiente`: código muerto. | `service/impl/RequerimientoServiceImpl.java:4-10` | ⚪ |

## B. Seguridad / autorización a nivel de recurso (§6.5)

| # | Hallazgo | Ubicación | Sev. |
|---|----------|-----------|------|
| S1 | ✅ **Resuelto (etapa 1): el Cliente solo ve las OT de sus Solicitudes (`OrdenRepository.findVisiblesPara`).** **Cliente ve todas las OT.** Tiene `orden.read`, y `puedeVer`/`listarOrdenes` solo filtran al rol Operaciones; cualquier otro rol (incluido Cliente) recibe `findAll()`. | `service/impl/OrdenServiceImpl.java:133-138, 370` | 🔴 |
| S2 | ✅ **Resuelto (etapa 1): sin `solicitud.read_all` solo se edita la propia y en "Pendiente".** **Cliente puede editar Solicitudes ajenas.** Tiene `solicitud.update` y `editarSolicitud` no verifica propietario (sí lo hacen los adjuntos). | `service/impl/SolicitudServiceImpl.java:132` | 🔴 |
| S3 | ✅ **Resuelto (etapa 1): Cliente sin `usuario.read`; historiales filtrados por visibilidad del registro padre.** Cliente tiene `usuario.read` + `historial_orden.read`: lista todos los usuarios (emails) y todo el historial de OT (`findAll()` sin filtro). | `config/DataSeeder.java` (matriz), `service/impl/UsuarioServiceImpl.java:77`, `service/impl/HistorialOrdenServiceImpl.java:60` | 🟠 |
| S4 | ✅ **Resuelto (etapa 1): el resumen por estado se recorta al alcance del actor.** `/solicitudes/resumen-estados` devuelve conteos globales también al Cliente. | `service/impl/SolicitudServiceImpl.java:183` | ⚪ |
| S5 | ✅ **Resuelto (etapa 1): RQ y sus adjuntos con visibilidad por recurso.** Requerimientos y Aprobaciones (listar, ver, adjuntos) sin ningún control por recurso; Operaciones lee todos los RQ. | `service/impl/RequerimientoServiceImpl.java:159-170, 247-280` | 🟡 |
| S6 | ✅ **Parcial (etapa 1): visibilidad por permisos de alcance `*.read_all` en `AutorizacionRecursoService`. Queda el chequeo de rol "Operaciones" al elegir ejecutor; desaparece en la etapa 3 (cola por especialidad).** Autorización por **nombre de rol hardcodeado** (`"Cliente"`, `"Operaciones"`, `"Administrador"`) en lugar de permisos (§6.2). Renombrar un rol rompe las reglas silenciosamente. | `SolicitudServiceImpl.java:406`, `OrdenServiceImpl.java:222, 367` | 🟡 |
| S7 | Permisos embebidos en el JWT al login: revocar un permiso no surte efecto hasta que expira el token (hasta `JWT_EXPIRATION`). Aceptable si se documenta. | `security/JwtAuthenticationFilter.java:52` | ⚪ |
| S8 | Swagger/OpenAPI público (`permitAll`) contra “no crear endpoints públicos por defecto”. Restringir por perfil. | `security/SecurityConfig.java:67` | 🟡 |
| S9 | Rate limit: el mapa `ventanas` nunca se purga (crece una entrada por IP) y usa `getRemoteAddr()` (detrás de un proxy todos comparten IP). | `security/RateLimitFilter.java:31-51` | 🟡 |
| S10 | ✅ **Resuelto (etapa 1): solo el ejecutor asignado cierra la OT.** `cerrarOrden`: cualquier rol no-Operaciones con `orden.cerrar` cierra cualquier OT (hoy solo Operaciones lo tiene; frágil si se reasignan permisos). | `service/impl/OrdenServiceImpl.java:174` | ⚪ |

| S11 | **`.env` versionado en git** con `JWT_SECRET` y la contraseña de la BD (en 3 commits). ✅ **Mitigado 2026-10-03:** `git rm --cached .env`, agregado a `.gitignore` y creado `.env.example`. ⚠️ **Pendiente del usuario:** rotar `JWT_SECRET` y la contraseña de la BD (siguen en el historial). | `.env`, `.gitignore` | 🔴 |

## C. Bugs

| # | Hallazgo | Ubicación | Sev. |
|---|----------|-----------|------|
| B1 | ✅ **Resuelto (etapa 1): el seeder ya no siembra activos sin especialidad; la app arranca con BD vacía (verificado).** **Regresión por el cambio actual:** se quitó la especialidad `Videovigilancia` del seed, pero `run()` sigue llamando `seedActivos(especialidades.get("Videovigilancia"))` → `null`. En una BD nueva, `Activo.especialidad` es `nullable=false` → falla el seeder transaccional y **la aplicación no arranca**. En la BD actual pasa desapercibido porque los activos ya existen. El Javadoc de `seedEspecialidades` quedó desactualizado. | `config/DataSeeder.java:94, 217` | 🔴 |
| B2 | ✅ **Resuelto (etapa 1): secuencias `seq_solicitud/requerimiento/orden` en `db/init.sql` + `NumeracionService`.** Correlativos `count() + 1` para ST/RQ/OT: duplican números con concurrencia y **tras borrar un registro** (`ST-5` se repite). Usar secuencia de BD. | `SolicitudServiceImpl.java:124, 246, 311`, `RequerimientoServiceImpl.java:118, 235`, `OrdenServiceImpl.java:100` | 🟠 |
| B3 | ✅ **Resuelto (etapa 1): Solicitud/RQ/OT con historial o dependencias → 409.** `deleteById` en Solicitud/RQ/OT sin verificar dependencias (historiales, OT, aprobaciones): la FK produce 500 en vez de 409; y borrar entidades auditables contradice la trazabilidad. | `SolicitudServiceImpl.java:178`, `RequerimientoServiceImpl.java:178`, `OrdenServiceImpl.java:161` | 🟡 |
| B4 | ✅ **Resuelto (etapa 1): `aprobado` e `id_requerimiento` obligatorios (`@NotNull`).** `crearAprobacion` con `aprobado = null` se trata como rechazo pero se persiste `null` en `aprobacion.aprobado`. Validar `@NotNull` en el DTO. | `service/impl/AprobacionServiceImpl.java:75, 89` | 🟡 |
| B5 | ✅ **Resuelto (etapa 1): FK inexistentes → 404.** `.orElse(null)` en FKs obligatorias (`estado`, `especialidad`) → `DataIntegrityViolation`/500 en vez de 404. | `OrdenServiceImpl.java:92-97`, `ActivoServiceImpl.java:36, 50` | 🟡 |
| B6 | ✅ **Resuelto (etapa 1): `url_adjunto` eliminado del body de generar OT.** Generar OT acepta `url_adjunto` desde el body, contradiciendo la regla “adjuntos solo vía fileserver” aplicada en el resto (y luego la respuesta expone `/api/archivos/ordenes/{id}` sobre una referencia que no es del fileserver). | `SolicitudServiceImpl.java:245`, `RequerimientoServiceImpl.java:234` | 🟡 |
| B7 | ✅ **Resuelto (etapa 1): `@Builder.Default` en las colecciones de `Usuario`.** `Usuario` usa `@Builder` con colecciones inicializadas sin `@Builder.Default`: las listas quedan `null` al construir con builder (Lombok lo advierte). | `model/Usuario.java:22, 51+` | ⚪ |
| B8 | ✅ **Resuelto (etapa 1): email corregido y normalizado (trim + minúsculas) al crear, editar e iniciar sesión.** Email sembrado con espacio final: `"aperalta@cfbd.co "` → ese usuario no puede iniciar sesión con su email real. | `config/DataSeeder.java:288` | 🟡 |
| B9 | ✅ **Aceptado por decisión D18: se mantiene `ddl-auto=update`; lo que Hibernate no crea va en `db/init.sql` idempotente.** `spring.jpa.hibernate.ddl-auto=update`: Hibernate puede alterar el esquema sin autorización (CLAUDE.md §3.3). Usar `validate` + migraciones (Flyway/Liquibase). | `src/main/resources/application.properties` | 🟡 |
| B10 | Contraseña de seed por defecto `ChangeMe123!` si falta `SEED_USERS_PASSWORD`. | `application.properties`, `config/DataSeeder.java` | ⚪ |

## D. Documentación (CLAUDE.md)

- ~~§8 tiene subsecciones numeradas `7.1` / `7.2`~~ ✅ corregido por el usuario (2026-10-03).
- ~~§8.2 indica `src/app/frontend/`~~ ✅ corregido por el usuario (2026-10-03).
- ~~§4: `security/` indentado bajo `config/`~~ ✅ corregido por el usuario (2026-10-03).
- `src/frontend/README.md` → “Observaciones” tiene la última línea incompleta (“se debe rechazar como …”) y plantea dividir “Soporte” en sub-especialidades: esto impacta N1/N2 y el seed de especialidades; conviene decidirlo antes de implementar la autoasignación.

---

## Prioridad sugerida

1. **B1** (bloquea el arranque en BD nueva) — fix de una línea.
2. **S1, S2, N7** — fugas/escaladas de acceso del Cliente.
3. **N1 + N2** — núcleo del nuevo flujo (autoasignación y reasignación por especialidad). Requiere aprobar el cambio de modelo `Usuario ↔ Especialidad`.
4. **N4, B2** — integridad del ciclo de vida y numeración.
5. Resto.

---

## Registro de cambios

> Regla vigente desde 2026-10-03: todo cambio de código (de Claude o del usuario) se registra aquí y se refleja en `docs/prd/`. Los hallazgos resueltos se marcan con ✅ en su tabla.

| Fecha | Autor | Cambio | Hallazgos / FR |
|---|---|---|---|
| 2026-10-03 | Usuario | `DataSeeder`: se quitó la especialidad "Videovigilancia" del seed | Origina **B1** |
| 2026-10-03 | Usuario | `RequerimientoServiceImpl`: se eliminó el comentario que explicaba la tabla `TRANSICIONES_PERMITIDAS` (sin cambio funcional) | — (relacionado con N10) |
| 2026-10-03 | Usuario | CLAUDE.md: numeración §8, ruta del frontend y árbol de §4 corregidos | Sección D ✅ |
| 2026-10-03 | Usuario | BD de desarrollo limpiada; se autoriza reescribir `DataSeeder` | D19 |
| 2026-10-03 | Claude | CLAUDE.md §1: diagrama de flujo de 14 pasos | — |
| 2026-10-03 | Claude | `.env` fuera de git, `.gitignore` y `.env.example` | S11 |
| 2026-10-03 | Claude | **Etapa 1**: `AutorizacionRecursoService` + permisos de alcance `solicitud/requerimiento/orden.read_all`; visibilidad por recurso en Solicitud, RQ, OT, historiales y adjuntos; `NumeracionService` + `db/init.sql`; borrados protegidos; validaciones; seeder con catálogo de 5 especialidades, estados "Asignada"/"Devuelta" y los 9 permisos aprobados; `nombre_usuario`/`nombre_ejecutor` en las respuestas; pruebas actualizadas (37/37 en verde) | N6, N7, S1–S6, S10, B1–B8 · FR-001…FR-007, FR-025, FR-038, FR-039, FR-041, FR-042 |
| 2026-10-03 | Claude | **Etapa 2**: entidad `UsuarioEspecialidad` (tabla `usuario_especialidad`), `EquipoEspecialidadService`, endpoints `GET /especialidades/mias`, `GET/PUT /especialidades/{id}/miembros` (solo usuarios cuyo rol tiene `orden.tomar`); pruebas 38/38 | N1 (parcial) · FR-008, FR-009, FR-011 |
| 2026-10-03 | Claude | **Etapa 3**: OT con ejecutor opcional (`orden.id_usuario` nullable vía `db/init.sql`), estados "Asignada"/"Devuelta", tabla `asignacion_orden`, `ColaOrdenService` (cola, tomar, asignar, verificar, reasignar, carga del equipo, asignaciones) con bloqueo de fila, `GeneradorOrdenService`, `RegistroHistorialService`, `OrdenMapper` (elimina 3 copias), aprobación con OT y rechazo que rechaza la Solicitud. **Cambios de contrato:** `generar-orden` recibe `id_especialidad` (ya no `id_usuario_ejecutor`); `reasignar` recibe `id_especialidad_destino` + `motivo`; el PUT de OT ya no cambia estados; cerrar exige "En progreso". `FlujoNegocioIntegrationTest` reescrito para los 14 pasos (11 pruebas, incluida concurrencia); suite 39/39 | N1–N5, S6 (parcial) · FR-013…FR-021, FR-023, FR-024 |
| 2026-10-03 | Claude | **Etapa 4 (backend)**: `GET /api/notificaciones/stream` (SSE, JWT en header), `SseEmitterRegistry` con heartbeat y limpieza, `NotificacionOrdenListener` (AFTER_COMMIT) para `orden.encolada` / `orden.asignada` / `orden.devuelta`; permiso ASYNC en `SecurityConfig`; variables `SSE_TIMEOUT_MINUTES` y `SSE_HEARTBEAT_SECONDS` en `.env`/`.env.example`; `NotificacionSseIntegrationTest`; suite 40/40 | FR-026, FR-027 (backend) |
| 2026-10-03 | Claude | **Etapa 5 (frontend)**: Órdenes con bandejas Cola / Mis órdenes / Mi equipo / Todas, acciones Tomar, Me corresponde / No me corresponde, Asignar, Reasignar especialidad y Cerrar, sección de asignaciones; despacho de Solicitudes y aprobación de RQ eligiendo especialidad (5 opciones, precargada); estado "Rechazado" visible para el Cliente con su motivo; administración de equipos en Especialidades; cliente SSE (`NotificacionStreamService`, fetch + header) con contador en el menú y avisos; historiales con nombre del actor (backend `nombre_usuario`). Build Angular OK; backend 40/40 | S6 · FR-022, FR-028, FR-029, FR-043…FR-048 |
| 2026-10-03 | Claude | **Etapa 6**: `GET /api/kpis/{familia}` y `/export.csv` (sla-despacho, tiempos-ciclo, cola-carga, ruteo) con rango de fechas (máx. 366 días, zona `KPI_ZONA_HORARIA`), alcance global con `orden.read_all` o recortado a las especialidades del responsable; calculadores por familia (`KpiCalculator`), `CsvExporter` con neutralización de fórmulas; dashboard con bloque "Mi trabajo" y panel de indicadores (rango, tarjetas, tabla, CSV). Backend 52/52; build Angular OK | FR-030…FR-037 |

| 2026-10-04 | Usuario | Entrega lista de equipos, especialidades por activo, nombres de 2 usuarios nuevos; aprueba escalado y metas de KPI; confirma que el Despachador no rechaza Solicitudes directamente | OQ-04, OQ-07, OQ-09, OQ-10 |
| 2026-10-04 | Claude | **Etapa 7**: (a) seed de equipos reales y de `olopez`/`ldesposorio`; tabla `activo_especialidad` (N:M, `activo.id_especialidad` = principal) con API, formulario de activos y despacho que muestra primero las especialidades del activo; 4 activos sembrados con sus especialidades. (b) Escalado de OT en cola por prioridad (`EscaladoColaJob`, `GET /ordenes/escaladas`, aviso SSE `orden.escalada`, lista en el dashboard). (c) Metas de KPI configurables (`MetasKpi`), SLA de atención por prioridad, KPI `sla-atencion` y `decision-rq`, indicadores `cola_p90` y `carga_max`; el panel muestra meta y estado. Pruebas: respaldo/restauración de equipos para no pisar datos reales; suite 59/59 | FR-012 · OQ-04, OQ-07, OQ-10 |

## Pendientes conocidos (al 2026-10-04, tras la etapa 7)

- ~~OQ-10: lista de equipos y especialidades por activo~~ ✅ sembrada el 2026-10-04.
- Las pruebas de integración escriben en la BD de desarrollo del `.env` (decisión del usuario, 2026-10-03).
- Siguen abiertos: N8–N10, S7, S8, S9 y B10.
- ~~El frontend quedó desalineado con el nuevo contrato de OT~~ ✅ corregido en la etapa 5.
- **Sin pruebas automatizadas del frontend nuevo** (solo hay 3 pruebas de scaffolding) y **sin verificación visual en navegador**: pendiente probar el flujo de punta a punta en la UI (FR-043…FR-048).
- **S11:** rotar `JWT_SECRET` y la contraseña de la BD (siguen en el historial de git).
- Siguen abiertas en el PRD: **OQ-05** (edición tras despacho) y **OQ-08** (catálogo de contratos). OQ-04, OQ-07, OQ-09 y OQ-10 se cerraron el 2026-10-04.
- El registro de escalados avisados vive en memoria: tras un reinicio, una OT ya escalada se vuelve a avisar una vez.
- La BD de desarrollo contiene datos creados por las pruebas de integración (solicitudes, OT, activos "Activo de prueba", clientes "cliente-ownership-…"), por la decisión D23.
- Los permisos nuevos viajan en el JWT: los usuarios ya logueados deben volver a iniciar sesión para obtenerlos (S7).
