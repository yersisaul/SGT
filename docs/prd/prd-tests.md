# Plan de pruebas — SGT

> Modo visión completa: las pruebas se definen como **especificaciones** (sin código). Se implementan al detallar cada épica. Esta especificación no incluye ninguna prueba "implementada" todavía: **0 implementadas · 48 especificadas**.

Herramientas: JUnit 5 + Mockito (unitarias) · Spring Boot Test + **Testcontainers PostgreSQL** (integración; no H2, porque se usan secuencias y `percentile_cont`) · MockMvc con JWT de prueba (API) · Vitest/Jasmine según el proyecto Angular (componentes) · Playwright (e2e). Datos 100 % sintéticos generados por factorías; nunca copias de producción. Cada prueba de integración usa una transacción revertida o un esquema limpio, y es independiente del orden de ejecución.

## Parte A — Pruebas de cobertura por capa

### Unitarias (servicio)
- `ColaOrdenServiceTest`: tomar (en cola, ya tomada, no miembro, OT cerrada), asignar (responsable, no responsable, destino que no es miembro), verificar (true/false, actor distinto), reasignar (sin motivo, a la misma especialidad, OT cerrada).
- `AutorizacionRecursoServiceTest`: matriz actor × recurso (Cliente propietario o ajeno, miembro o no de la especialidad, responsable, Admin), basada en permisos y no en nombres de rol.
- `GeneradorOrdenServiceTest`: desde Solicitud y desde RQ, número de OT por secuencia, asignación ENCOLADA registrada.
- `AprobacionServiceTest`: aprobar con y sin especialidad de OT, rechazar con Solicitud de origen y sin ella, `aprobado` nulo.
- Calculadores de KPI: un test por fórmula con datos sintéticos de resultado conocido (SLA, mediana, p90, tasa de reasignación, cola).
- `CsvExporterTest`: escape de comillas, separador, UTF-8 y neutralización de `= + - @`.

### Integración (Testcontainers)
- Arranque doble sobre una BD vacía: seeder y `db/init.sql` idempotentes (NFR-009).
- Concurrencia: 2 hilos toman la misma OT y exactamente uno obtiene 1 fila actualizada.
- Concurrencia: 50 creaciones paralelas de Solicitud generan números únicos.
- Transaccionalidad: un fallo forzado al crear la OT en "aprobar + generar" no persiste ni la aprobación ni el cambio de estado.
- `SseEmitterRegistry`: entrega solo a los destinatarios, heartbeat y limpieza de conexiones cerradas.

### API (MockMvc)
- Cada endpoint nuevo: 401 sin token, 403 sin permiso, 200/201 con permiso, 400 con body inválido (`details` presente), 429 al superar el límite.
- El error nunca expone trazas, SQL ni rutas internas.

### Seguridad
- SAST: SpotBugs + FindSecBugs en CI. SCA: OWASP Dependency-Check y `npm audit`, que bloquean ante CVE altas o críticas.
- DAST: OWASP ZAP baseline contra el entorno de staging.
- OWASP Top 10: IDOR (cambiar UUID ajeno en cada endpoint por recurso), inyección en filtros de KPI, XSS almacenado en motivo y descripción mostrados en el timeline, inyección de fórmulas en CSV.
- Pentest previo a producción sobre el flujo de OT y los KPIs.

### Rendimiento
- k6: 50 conexiones SSE con 10 eventos/min; p95 de entrega < 5 s (NFR-003).
- k6: KPIs sobre 50 000 OT sintéticas en un rango de 90 días; p95 < 1,5 s (NFR-004).
- Comparación contra la línea base en cada versión; una degradación > 20 % hace fallar el pipeline.

### Frontend y e2e
- Componentes: cola (acciones según el estado), modal de aprobación (preselección, cancelar), timeline, badge que reacciona a eventos.
- e2e (Playwright): flujo completo de los pasos 1 a 14 con usuarios sembrados sintéticos; flujo de rechazo; reasignación entre especialidades.
- Accesibilidad: axe-core en cada vista nueva (NFR-011).

## Parte B — Pruebas de validación de AC

| Prueba | AC | Tipo | Especificación |
|---|---|---|---|
| T-001 | AC-001 | Integración | Arranque contra un contenedor vacío; se verifica que todos los activos tienen una especialidad válida |
| T-002 | AC-002 | API | POST de Solicitud con `id_estado` Finalizado → estado guardado Pendiente |
| T-003 | AC-003 | API | PUT de un cliente ajeno → 403/404; PUT del dueño en "En progreso" → 409 |
| T-004 | AC-004 | API | El cliente A lista OT → solo las de A; GET de una OT de B → 404 |
| T-005 | AC-005 | API | El Cliente lista historial de OT y usuarios → recortado o 403 |
| T-006 | AC-006 | Integración | Renombrar el rol y repetir T-004 → mismo resultado |
| T-007 | AC-007 | API | Cambiar la especialidad de una Solicitud con OT → 409 |
| T-008 | AC-008 | Integración | Usuario en 2 especialidades → la cola incluye OT de ambas |
| T-009 | AC-009 | API | Dos responsables asignan en su especialidad (200) y fuera de ella (403) |
| T-010 | AC-010 | API | GET de especialidades → exactamente las 5 del catálogo D4 |
| T-011 | AC-011 | Integración | Quitar el miembro → desaparece la cola y se conservan sus OT asignadas |
| T-012 | AC-012 | Integración | Dos arranques sobre BD vacía con la lista de equipos → asignaciones correctas y sin duplicados |
| T-013 | AC-013 | API | Generar OT → usuario null, especialidad elegida y ENCOLADA registrada |
| T-014 | AC-014 | Integración | Dos tomas concurrentes → 200 y 409; un no miembro → 403 |
| T-015 | AC-015 | API | El responsable asigna a un miembro (200) y a un no miembro (409) |
| T-016 | AC-016 | API | Verificar con true → En progreso; con false + motivo → Devuelta sin ejecutor y tomar → 409; sin motivo → 400; otro actor → 403 |
| T-017 | AC-017 | API | Responsable o Admin reasigna → cambia la cola; sin motivo → 400; miembro no responsable → 403; misma especialidad → 409 |
| T-018 | AC-018 | Integración | Registros DEVUELTA y REASIGNADA_ESPECIALIDAD con todos sus campos |
| T-019 | AC-019 | API | Cerrar por un no ejecutor → 403; por el ejecutor → OT y origen Finalizado |
| T-020 | AC-020 | API | Respuestas de cola, mis OT y equipo por actor |
| T-021 | AC-021 | Integración | Rechazar el RQ → RQ y Solicitud "Rechazado" con el motivo en el historial |
| T-022 | AC-022 | e2e | El Cliente ve "Rechazado" y el motivo en su seguimiento |
| T-023 | AC-023 | Integración | Aprobar con especialidad → OT en cola y estados; fallo forzado → rollback total |
| T-024 | AC-024 | API | Aprobar sin especialidad → RQ Aprobado y Solicitud En revisión; generar OT manual → 201 |
| T-025 | AC-025 | API | `aprobado` nulo → 400 con detalle del campo |
| T-026 | AC-026 | Integración | Evento `orden.encolada` entregado a los miembros y no a los ajenos, en < 5 s |
| T-027 | AC-027 | Integración | Evento `orden.asignada` sin PII |
| T-028 | AC-028 | Componente | El badge incrementa al recibir el evento |
| T-029 | AC-029 | e2e | Corte de red simulado → reconexión y contadores coherentes |
| T-030 | AC-030 | Unitaria | SLA con 9/10 dentro del plazo → 90 % |
| T-031 | AC-031 | Unitaria | Tiempos [1 h, 2 h, 3 h] → mediana 2 h |
| T-032 | AC-032 | Integración | Cola de 4 OT, la más antigua de 6 h |
| T-033 | AC-033 | Unitaria | 3 reasignadas de 20 → 15 % |
| T-034 | AC-034 | e2e | Dashboard de cada rol con sus widgets |
| T-035 | AC-035 | API | Rango válido → recalculado; `desde > hasta` o > 366 días → 400 |
| T-036 | AC-036 | Integración | El CSV coincide con la API, en UTF-8 y con fórmulas neutralizadas |
| T-037 | AC-037 | API | Responsable fuera de su ámbito → 403 o vacío; Cliente → 403 |
| T-038 | AC-038 | Integración | 50 altas paralelas y un borrado → números únicos |
| T-039 | AC-039 | API | DELETE de una Solicitud con historial → 409 con `code` |
| T-040 | AC-040 | Integración | Secuencias e índice existen tras el arranque; segundo arranque sin error |
| T-041 | AC-041 | API | FK inexistente → 404 |
| T-042 | AC-042 | API | `url_adjunto` en el body de generar OT → ignorado o 400 |
| T-043 | AC-043 | e2e | Bandeja ordenada por vencimiento; sin selector de persona |
| T-044 | AC-044 | e2e | Pestañas y acciones de Operaciones según el estado |
| T-045 | AC-045 | e2e | El responsable asigna desde la vista de Equipo |
| T-046 | AC-046 | Componente | El modal lista 5 especialidades con la del RQ preseleccionada; cancelar aprueba sin OT y muestra "Generar OT" |
| T-047 | AC-047 | Componente | El timeline muestra estados, fechas y motivo |
| T-048 | AC-048 | e2e | Alta de responsable persistida y efectiva |

## Parte C — Matriz de trazabilidad

| FR | AC | Prueba |
|---|---|---|
| FR-001 … FR-048 | AC-001 … AC-048 (1:1) | T-001 … T-048 (1:1) |

Cobertura: **48/48 FR con AC · 48/48 AC con prueba especificada**. Los NFR se validan con las pruebas de la Parte A: NFR-003/004 (k6), NFR-005 (concurrencia), NFR-006/007 (seguridad y CSV), NFR-008 (registro SSE), NFR-009 (migraciones), NFR-010 (JaCoCo), NFR-011 (axe-core).

Objetivos de cobertura: 80 % de líneas y 70 % de ramas en los servicios del flujo (NFR-010); 100 % de los endpoints nuevos con pruebas 401/403/400/2xx.
