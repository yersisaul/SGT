# Especificación técnica (nivel arquitectura) — SGT

> Modo visión completa: modelo de datos **conceptual** (sin DDL), contratos de API resumidos y decisiones de diseño. El DDL definitivo se escribe al detallar cada épica y **requiere aprobación** (CLAUDE §3.3).

## 1. Arquitectura vigente que se respeta

El código usa **capas MVC** (`controller → service/impl → repository → model`, DTOs en `dto/request|response`). Este PRD **no** introduce otra arquitectura (CLAUDE §3.1). Las reglas nuevas se añaden como componentes con una sola responsabilidad dentro de `service/` y `security/`, inyectados por constructor:

| Componente nuevo | Capa | Responsabilidad única |
|---|---|---|
| `AutorizacionRecursoService` | service | Decide si el actor ve o modifica un recurso concreto (Solicitud, RQ, OT, historial) a partir de los permisos de alcance `*.read_all` (D20) y de su relación con el recurso. Sustituye los `puedeVer` y `esCliente/esOperaciones` duplicados en cada servicio (AUD-S6). **Implementado (etapa 1)** |
| `UsuarioActualProvider` | security | Identidad y permisos del usuario autenticado desde el `SecurityContext`. **Implementado (etapa 1)** |
| `NumeracionService` | service | Números ST/RQ/OT desde secuencias de PostgreSQL. **Implementado (etapa 1)** |
| `ColaOrdenService` | service | Encolar, tomar, asignar, verificar y reasignar OT. Es el único que muta `orden.usuario` y `orden.especialidad` |
| `AsignacionOrdenRecorder` | service | Persiste los eventos de asignación (FR-018). Lo usa solo `ColaOrdenService` |
| `GeneradorOrdenService` | service | Crea la OT en cola a partir de una Solicitud o un RQ. Hoy esta lógica está duplicada en `SolicitudServiceImpl` y `RequerimientoServiceImpl` (`convertirOrdenAResponse` y `resolverEjecutorOperaciones`, ambos duplicados) |
| `NotificacionPublisher` (interfaz) y `SseNotificacionPublisher` | service / config | Publica eventos de dominio tras el commit; la implementación SSE es reemplazable (Open/Closed) |
| `SseEmitterRegistry` | config | Registro en memoria de conexiones por usuario, con heartbeat y limpieza |
| `KpiQueryService` y una clase `*KpiCalculator` por familia | service | Cálculo de cada familia de KPI con una consulta agregada parametrizada |
| `CsvExporter` | service | Serializa filas de KPI a CSV (escapa comillas y neutraliza fórmulas `= + - @`) |

Visibilidad: las clases `*ServiceImpl` y los calculadores son *package-private* cuando no se consumen fuera de `service.impl`; solo las interfaces de `service/` son públicas. Los controladores dependen de interfaces, nunca de implementaciones (inversión de dependencias, ya vigente en el proyecto).

## 2. Modelo de datos (conceptual)

| Cambio | Tipo | Detalle | Requiere aprobación |
|---|---|---|---|
| `usuario_especialidad` | Tabla nueva | `id_usuario_especialidad` UUID PK · `id_usuario` FK · `id_especialidad` FK · `es_responsable` boolean NOT NULL default false · único (`id_usuario`, `id_especialidad`) | Sí (aprobado) |
| `orden.id_usuario` | Columna existente pasa a **nullable** | `null` = OT en cola o devuelta. Hoy es `nullable=false` en `model/Orden.java` | Sí (aprobado) |
| `asignacion_orden` | Tabla nueva | `id_asignacion_orden` UUID PK · `id_orden` FK · `tipo` (ENCOLADA, TOMADA, ASIGNADA, CONFIRMADA, DEVUELTA, REASIGNADA_ESPECIALIDAD) · `id_especialidad_origen` FK nullable · `id_especialidad_destino` FK · `id_usuario_origen` FK nullable · `id_usuario_destino` FK nullable · `id_usuario_actor` FK · `motivo` · `fecha` | Sí (aprobado) |
| `orden.version` | No implementada | Se reemplazó por **bloqueo de fila** (`PESSIMISTIC_WRITE` en `OrdenRepository.findByIdParaActualizar`) para todas las operaciones de la cola; cubre también reasignar y cerrar | — |
| Secuencias `seq_solicitud`, `seq_requerimiento`, `seq_orden` | Nuevas (en `db/init.sql`) | Reemplazan `count()+1` (AUD-B2) | Sí (aprobado) |
| Estados "Asignada" y "Devuelta" | Solo de datos | Catálogo de `estado`, usados solo por OT (D13) | No |
| Estado "Rechazado" en Solicitud | Solo de datos | El estado ya existe en el catálogo (`DataSeeder.seedEstados`); se habilita para Solicitud | No |
| Especialidades | Solo de datos | Catálogo de 5 (D4) sembrado sobre la BD limpia (D19); sin migración de OT | No |

**Por qué una tabla de asignaciones aparte y no comentarios en `historial_orden`:** hoy la reasignación se documenta como transición "no-op" con texto libre (`OrdenServiceImpl.reasignarOrden`). Los KPI de ruteo (FR-033) y de tiempo en cola (FR-031) necesitan campos consultables, no texto.

**Esquema (D18):** se mantiene `spring.jpa.hibernate.ddl-auto=update`. Hibernate crea y altera tablas y columnas; lo que no puede crear (secuencias de numeración e índice parcial de la cola) va en `src/main/resources/db/init.sql` con sentencias `CREATE … IF NOT EXISTS`, ejecutado al arrancar después de Hibernate (`spring.sql.init.mode=always` + `spring.jpa.defer-datasource-initialization=true`). Es idempotente (NFR-009).

**Clasificación de datos**

| Entidad o campo | Clase | Manejo |
|---|---|---|
| `usuario.password_hash` | Restringido | BCrypt (`SecurityConfig.passwordEncoder`); nunca se expone en DTO, log ni CSV |
| `usuario.email`, nombres | Confidencial (PII) | Visible para Admin; en CSV solo para Admin (NFR-007); no se registra en logs de KPI |
| Solicitud, RQ, OT y descripciones | Interno | Visibilidad por recurso (`AutorizacionRecursoService`) |
| Historiales y `asignacion_orden` | Interno, auditoría | Solo inserción; sin endpoints de edición ni borrado |
| KPIs agregados | Interno | Recortados por ámbito del actor (FR-037) |

## 3. Máquina de estados de la OT (D13, D15)

```
 generar OT (FR-013)
        │
        ▼
   PENDIENTE  (en cola, usuario = null) ◄──────── reasignar a otra especialidad
        │                                         (responsable o Admin, FR-017;
        │ tomar (miembro) /                        desde cualquier estado abierto)
        │ asignar (responsable)
        ▼
   ASIGNADA  (usuario = ejecutor) ── verificar: NO corresponde ──► DEVUELTA (usuario = null,
        │                                                          misma especialidad)
        │ verificar: SÍ corresponde                                   │
        ▼                                                             │ responsable: asignar a
   EN PROGRESO                                                        │ otro miembro → ASIGNADA
        │ cerrar (solo el ejecutor)
        ▼
   FINALIZADO → Solicitud / RQ de origen → Finalizado
```

| Estado | `usuario` | Quién actúa |
|---|---|---|
| Pendiente | null | Cualquier miembro la **toma**; el responsable la **asigna**; responsable o Admin la **reasignan** a otra especialidad |
| Asignada | ejecutor | El ejecutor la **verifica**; responsable o Admin la reasignan |
| Devuelta | null | El responsable la asigna a otro miembro o la reasigna; el Admin la reasigna. Los miembros no pueden tomarla |
| En progreso | ejecutor | El ejecutor la **cierra**; responsable o Admin la reasignan |
| Finalizado | ejecutor | Nadie (inmutable) |

## 4. API (contratos resumidos)

Todos los endpoints exigen `Authorization: Bearer <JWT>`, un permiso declarado con `@PreAuthorize` y pasan por el `RateLimitFilter` (NFR-001, NFR-002). **Ninguno es público.** Los permisos en *cursiva* son nuevos y requieren autorización (OQ-01).

| Método y ruta | Permiso | Regla por recurso | Respuestas |
|---|---|---|---|
| `POST /api/solicitudes/{id}/generar-orden` | `solicitud.generar_orden` | Solicitud en "Pendiente". Body: `{ id_especialidad, comentario? }` (se elimina `id_usuario_ejecutor` y `url_adjunto`) | 201 OrdenResponse · 409 estado · 404 |
| `POST /api/aprobaciones` | `requerimiento.aprobar` | Body: `{ id_requerimiento, aprobado (obligatorio), comentario, id_especialidad_orden? }`. Si `aprobado` es true y llega `id_especialidad_orden`, se genera la OT en la misma transacción (FR-023); sin ella, solo se aprueba (D17, modal cancelado) | 201 · 400 · 409 |
| `POST /api/requerimientos/{id}/generar-orden` | `requerimiento.generar_orden` | RQ "Aprobado" sin OT (camino manual de FR-024). Body: `{ id_especialidad, comentario? }` | 201 · 409 |
| `GET /api/ordenes/cola` | `orden.read` | OT "Pendiente" sin ejecutor de las especialidades del actor; para responsables también las "Devuelta". **Implementado (etapa 3)** | 200 |
| `GET /api/ordenes/equipo/{idEspecialidad}` | `orden.asignar` o `orden.read_all` | Carga por miembro (OT abiertas). Solo responsable de la especialidad o alcance global. **Implementado (etapa 3)** | 200 · 403 · 404 |
| `GET /api/ordenes/{id}/asignaciones` | `orden.read` | Eventos de asignación de la OT (visibilidad de la OT). **Implementado (etapa 3)** | 200 · 404 |
| `POST /api/ordenes/{id}/tomar` | *`orden.tomar`* | El actor es miembro de `orden.especialidad`; la OT sigue en cola | 200 · 403 · 409 (ya tomada) |
| `POST /api/ordenes/{id}/asignar` | *`orden.asignar`* | El actor es responsable de `orden.especialidad`; la OT está "Pendiente" o "Devuelta"; el destino es miembro de esa especialidad. Body: `{ id_usuario }` | 200 · 403 · 409 |
| `POST /api/ordenes/{id}/verificar` | *`orden.verificar`* | El actor es el ejecutor y la OT está "Asignada". Body: `{ corresponde: boolean, motivo? }` (motivo obligatorio si es false). false → "Devuelta" sin ejecutor | 200 · 400 · 403 · 409 |
| `POST /api/ordenes/{id}/reasignar` | `orden.reasignar` (existente) | **Cambia el contrato**: body `{ id_especialidad_destino, motivo (obligatorio) }`. Solo el responsable de la especialidad actual o el Admin (D14); la OT queda "Pendiente" en la cola destino | 200 · 400 · 403 · 409 |
| `POST /api/ordenes/{id}/cerrar` | `orden.cerrar` | Solo el ejecutor asignado (endurece AUD-S10) | 200 · 403 · 409 |
| `GET /api/especialidades/mias` | `especialidad.read` | Especialidades del usuario autenticado y si es responsable. **Implementado (etapa 2)** | 200 |
| `GET /api/especialidades/{id}/miembros` | `especialidad.gestionar_equipo` o `orden.asignar` | Miembros (sin email) y `es_responsable`. **Implementado (etapa 2)** | 200 · 404 |
| `PUT /api/especialidades/{id}/miembros` | *`especialidad.gestionar_equipo`* | Reemplaza el equipo completo. Body `{ miembros: [{ id_usuario, es_responsable }] }`. Solo usuarios cuyo rol tiene `orden.tomar`; sin duplicados. **Implementado (etapa 2)** | 200 · 400 · 404 · 409 |
| `GET /api/kpis/{familia}?desde&hasta&id_especialidad?` | *`kpi.read`* | familia ∈ {sla-despacho, tiempos-ciclo, cola-carga, ruteo}; `desde`/`hasta` en `yyyy-MM-dd` inclusive (por defecto últimos 30 días, zona `KPI_ZONA_HORARIA`); global con `orden.read_all`, si no solo especialidades donde el actor es responsable (FR-037). Respuesta genérica `{ indicadores, columnas, filas }`. **Implementado (etapa 6)** | 200 · 400 · 403 · 404 |
| `GET /api/kpis/{familia}/export.csv?…` | *`kpi.export`* | Igual que el anterior; `text/csv; charset=UTF-8`, UTF-8 con BOM, separador coma. **Implementado (etapa 6)** | 200 · 400 · 403 |
| `GET /api/notificaciones/stream` | `orden.read` (todos los roles la tienen; solo entrega eventos dirigidos al propio usuario) | `Content-Type: text/event-stream`. Eventos `orden.encolada` (miembros), `orden.asignada` (ejecutor), `orden.devuelta` (responsables). **Implementado (etapa 4)** | 200 stream · 401 |

**Deprecación:** el body actual de `/ordenes/{id}/reasignar` (`id_usuario_nuevo`) y el `id_usuario_ejecutor` de generar OT dejan de aceptarse. Como el único consumidor es el frontend del mismo repositorio, el cambio se publica junto con E8 sin periodo de convivencia. Si aparecen consumidores externos, se versionará la ruta como `/api/v2`.

**Formato de error:** se mantiene el `ErrorResponse { status, message }` actual (`GlobalExceptionHandler`) y se agrega `code` (máquina, p. ej. `ORDEN_YA_TOMADA`) y `details[]` (errores por campo). No se exponen trazas ni SQL.

**Validación de entrada:** Bean Validation en los DTOs (`@NotNull`, `@Size(max=1000)` para motivo y comentario, UUID válidos). El rango de KPI se valida en el controlador (`desde ≤ hasta`, máximo 366 días). Cualquier violación responde 400 con `details`.

## 5. Concurrencia: tomar y asignar

Requisito NFR-005: dos personas que toman la misma OT a la vez → una gana. Se usa una **actualización condicional parametrizada** (sin concatenar strings), más `@Version` como segunda defensa:

```java
// OrdenRepository
@Modifying
@Query("UPDATE Orden o SET o.usuario = :ejecutor, o.version = o.version + 1 "
     + "WHERE o.id_orden = :idOrden AND o.usuario IS NULL AND o.fecha_cierre IS NULL")
int tomarSiEstaEnCola(@Param("idOrden") UUID idOrden, @Param("ejecutor") Usuario ejecutor);
```

```java
// ColaOrdenServiceImpl (extracto)
private static final int FILAS_ESPERADAS_AL_TOMAR = 1;

@Transactional
public OrdenResponse tomar(UUID idOrden) {
    Usuario actor = actorActual.obtener();
    Orden orden = buscarAbierta(idOrden);
    autorizacion.exigirMiembro(actor, orden.getEspecialidad());
    if (ordenRepository.tomarSiEstaEnCola(idOrden, actor) != FILAS_ESPERADAS_AL_TOMAR) {
        throw new ConflictoNegocioException(CodigoError.ORDEN_YA_TOMADA);
    }
    asignaciones.registrarToma(orden, actor);
    eventos.publicar(new OrdenTomadaEvent(idOrden, orden.getEspecialidad().getId_especialidad()));
    return mapper.toResponse(orden);
}
```
Aislamiento: `READ COMMITTED` (predeterminado de PostgreSQL). La condición `usuario IS NULL` en el `UPDATE` hace que la operación sea atómica sin bloqueo pesimista. Los eventos se publican con `@TransactionalEventListener(phase = AFTER_COMMIT)` para no avisar de algo que luego se revierte.

**Transacciones de negocio:** `aprobar + generar OT` (FR-023), `rechazar RQ + rechazar Solicitud` (FR-021) y `reasignar + registrar asignación` se ejecutan cada una en una sola transacción. Si falla cualquier paso, se revierte todo; no hay sagas porque todo vive en la misma BD.

## 6. Notificaciones SSE (E5)

| Decisión | Motivo |
|---|---|
| `SseEmitter` de Spring MVC (sin dependencias nuevas) | El proyecto es servlet/MVC; WebFlux no se justifica |
| **El token no viaja en la URL** (NFR-006) | `EventSource` del navegador no permite headers. El frontend abrirá el stream con `fetch` + `ReadableStream` enviando `Authorization: Bearer`, sin librería externa. Así el JWT nunca aparece en logs de acceso |
| Heartbeat (comentario `:ping`) cada 25 s y timeout del emitter de 30 min con reconexión del cliente | Evita cortes de proxies y limpia conexiones zombis (NFR-008) |
| Destinatarios: miembros de la especialidad (OT encolada o reasignada) y ejecutor asignado | FR-026, FR-027 |
| Payload mínimo: `{ tipo, id_orden, numero_orden, id_especialidad }` | Sin PII; el cliente pide el detalle a la API con su permiso |
| Una sola instancia (registro en memoria) | Alcance actual. Escalar a varias instancias requiere un broker (Redis pub/sub o `LISTEN/NOTIFY` de Postgres); está fuera de alcance |
| Degradación | Si el stream no está disponible, el badge se resincroniza al navegar y con un refresco de respaldo cada 60 s (FR-029). La operación del flujo no depende del SSE |

El `RateLimitFilter` cuenta la apertura del stream como 1 solicitud. Las reconexiones usan backoff exponencial con jitter (1 s, 2 s, 4 s… hasta 30 s) para no disparar 429.

## 7. KPIs (E6)

| Familia | Cálculo | Fuente de datos |
|---|---|---|
| SLA de despacho | % de Solicitudes cuyo primer historial con `estado_nuevo ≠ Pendiente` tiene `fecha ≤ fecha_registro + SLA(prioridad)` | `historial_solicitud`, `SlaCalculator` (`app.sla.*`) |
| Tiempos de ciclo | Mediana y p90 (`percentile_cont`) de: registro→despacho, ENCOLADA→TOMADA/ASIGNADA, TOMADA→cierre y registro→cierre | `historial_solicitud`, `asignacion_orden`, `orden.fecha_cierre` |
| Cola y carga | OT con `usuario IS NULL` por especialidad, antigüedad máxima y OT abiertas por ejecutor | `orden`, `asignacion_orden` |
| Ruteo | OT con ≥ 1 `REASIGNADA_ESPECIALIDAD` / OT generadas; RQ aprobados / RQ resueltos | `asignacion_orden`, `aprobacion` |

- Consultas agregadas con parámetros (`:desde`, `:hasta`, `:idsEspecialidad`); nunca concatenación de strings.
- Índices propuestos: `asignacion_orden(id_orden, fecha)`, `asignacion_orden(tipo, fecha)`, `historial_solicitud(id_solicitud, fecha)`, `orden(id_especialidad) WHERE usuario IS NULL` (predicado estable, sin funciones volátiles).
- Los calculadores implementan una interfaz común `KpiCalculator`: agregar una familia nueva no modifica las existentes (Open/Closed).
- CSV: `CsvExporter` neutraliza las celdas que empiezan con `= + - @` (inyección de fórmulas en Excel).

## 8. Frontend (E8)

Angular 21 con la estructura `core/ shared/ presentation/` existente (`src/frontend/src/app`).

| Pieza | Ubicación | Nota |
|---|---|---|
| `NotificacionStreamService` | `core/services` | `fetch` + parser SSE, reconexión con backoff, expone `signal` de contadores |
| `ColaOrdenService` (HTTP) | `core/services` | Tomar, asignar, verificar y reasignar |
| `KpiService` | `core/services` | Reemplaza `DashboardService` (que hoy solo llama a `resumen-estados`) |
| Dashboards por rol | `presentation/views/dashboard/<rol>` | Se elige por **permisos** del JWT, no por nombre de rol |
| Bandejas | `presentation/views/ordenes/cola`, `…/equipo`; `solicitudes/bandeja` | — |
| Modal de aprobación | `presentation/views/requerimientos/components/aprobar` | D7 |
| Timeline del Cliente | `shared/components/timeline` | Reutilizable para Solicitud, RQ y OT |
| Gráficos | Pendiente de elegir librería (OQ técnica): preferir una sin dependencias transitivas pesadas y con licencia MIT | Regla de dependencias mínimas |

Diseño visual: aplicar `ui-ux-pro-max` y `impeccable` (CLAUDE §8).

## 9. Observabilidad

- Logs estructurados (JSON en producción vía `logback` con encoder JSON) con `requestId` (filtro MDC), `userId` (UUID, nunca email) y `operacion`. No se registran JWT, contraseñas ni descripciones de solicitudes.
- Niveles: INFO para eventos de negocio (OT tomada o reasignada), WARN para 409 repetidos o reconexiones SSE excesivas, ERROR para fallos no controlados.
- Métricas con Micrometer/Actuator: **no implementado** (dependencia nueva sin aprobar). `SseEmitterRegistry.conexionesAbiertas()` queda disponible para exponerlo si se aprueba.
- Alertas propuestas: cola de una especialidad > N OT durante 2 h (P3, para el responsable); tasa de 5xx > 2 % en 5 min (P2, para el equipo técnico). El runbook se escribe en `docs/runbooks/` dentro de E7.
- Trazas distribuidas: no aplican (monolito con una sola instancia). Si se divide el sistema, se adoptará OpenTelemetry con W3C Trace Context.

## 10. Seguridad (resumen)

- JWT HS con secreto desde `JWT_SECRET` (≥ 32 caracteres, ya validado). TLS 1.2+ en el proxy de entrada y HSTS en producción.
- Autorización en dos niveles: `@PreAuthorize` (operación) y `AutorizacionRecursoService` (recurso).
- Permisos en el JWT: un cambio de permisos se aplica al renovar el token (AUD-S7, aceptado y documentado).
- Escaneo de dependencias: OWASP Dependency-Check (Maven) y `npm audit` en CI, que bloquean ante CVE altas o críticas.

## 11. Ampliaciones del 2026-10-04 (etapa 7)

| Cambio | Detalle |
|---|---|
| `activo_especialidad` (tabla nueva, aprobada) | `id_activo_especialidad` UUID PK · `id_activo` FK · `id_especialidad` FK · único (`id_activo`, `id_especialidad`). `activo.id_especialidad` se conserva como **principal**. API: `ActivoRequest/Response.ids_especialidad` |
| Escalado (sin cambios de BD) | `UmbralesEscalado` (`ESCALADO_*_MINUTOS`), `EscaladoColaService.detectar()` calcula el nivel desde la última entrada a la cola (`asignacion_orden`); `EscaladoColaJob` (cada `ESCALADO_REVISION_SEGUNDOS`) avisa una vez por (OT, nivel, entrada) por SSE `orden.escalada`: nivel 1 a responsables, nivel 2 a usuarios con `orden.read_all` + `orden.reasignar`. `GET /api/ordenes/escaladas` (`orden.asignar` o `orden.read_all`) |
| SLA de atención | `SlaCalculator.deadlineAtencion` (`SLA_ATENCION_*_HORAS`); KPI `sla-atencion` excluye Solicitudes que generaron RQ |
| Decisión de RQ | KPI `decision-rq`: primera aprobación/rechazo vs `KPI_DECISION_RQ_HORAS` |
| Metas | `MetasKpi` (variables `KPI_META_*`) agrega `meta` y `estado` (`cumple`/`alerta`/`no_cumple`) a `KpiIndicadorResponse` en `KpiServiceImpl` |
