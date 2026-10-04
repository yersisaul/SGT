# Requerimientos — SGT

Convención de fuentes: **Usuario** = pedido inicial · **R1/R2/R3** = ronda de clarificación · **Código (ruta:línea)** = hallazgo del análisis · **AUD-xx** = `docs/AUDITORIA.md` · **CLAUDE §x** = regla del proyecto.

## 1. Requerimientos funcionales

### E1 — Correcciones críticas y seguridad por recurso

| ID | Requerimiento | Prioridad | Depende de | Fuente |
|---|---|---|---|---|
| FR-001 | El seeder debe arrancar en una BD vacía: los activos se siembran con una especialidad existente del nuevo catálogo | P0 | — | AUD-B1 · Código (`config/DataSeeder.java:94`) |
| FR-002 | Una Solicitud nueva siempre nace en estado "Pendiente" (se ignora cualquier estado enviado) y con la especialidad de su activo (el Cliente no la elige) | P0 | — | AUD-N7 · Código (`SolicitudServiceImpl.java:116`) · R7 (D22) |
| FR-003 | Un usuario con rol Cliente solo puede editar sus propias Solicitudes, y solo mientras estén en "Pendiente" | P0 | — | AUD-S2 · CLAUDE §6.5 |
| FR-004 | El Cliente solo ve las OT originadas por sus propias Solicitudes (directas o vía RQ) | P0 | — | AUD-S1 · CLAUDE §1 paso 2 |
| FR-005 | Historiales, usuarios, RQ y aprobaciones aplican filtro por recurso según el actor (propietario, especialidad o global) | P1 | FR-004 | AUD-S3, AUD-S5 |
| FR-006 | Las reglas de visibilidad se centralizan en un componente de autorización por recurso y se basan en permisos de alcance (`*.read_all`), no en nombres de rol | P1 | FR-003, FR-004 | AUD-S6 · CLAUDE §6.2/6.4 · R6 (D20) |
| FR-007 | La Solicitud no admite cambios de especialidad ni de activo una vez despachada (OT o RQ generados) | P1 | — | AUD-N6 |

### E2 — Especialidades y equipos

| ID | Requerimiento | Prioridad | Depende de | Fuente |
|---|---|---|---|---|
| FR-008 | Un usuario puede pertenecer a una o más especialidades (relación N:M) | P0 | — | R1 (D2) |
| FR-009 | Cada pertenencia indica si el usuario es responsable de esa especialidad; puede haber varios responsables | P0 | FR-008 | R2 (D5) |
| FR-010 | El catálogo de especialidades es: Desarrollo, Implementación, DevOPS, Soporte y mantenimiento de código, Soporte de infraestructura y configuración de analíticas | P0 | — | R1 (D4) · `src/frontend/README.md` |
| FR-011 | El Administrador gestiona los miembros y responsables de cada especialidad desde la administración | P1 | FR-008, FR-009 | R2 (D5) |
| FR-012 | El seeder siembra el catálogo vigente, los miembros y responsables de cada especialidad y la especialidad de cada activo según la lista del dueño del producto (sin datos inventados) | P0 | FR-010 | Código (diff de `DataSeeder`) · R4/R5 (D19, OQ-10) |

### E3 — Cola de OT por especialidad

| ID | Requerimiento | Prioridad | Depende de | Fuente |
|---|---|---|---|---|
| FR-013 | Generar una OT (desde Solicitud o RQ) la asigna a una especialidad y la deja **en cola**, sin ejecutor | P0 | FR-008 | CLAUDE §1 pasos 5 y 9 · R1 (D3) |
| FR-014 | Cualquier miembro de la especialidad puede **tomar** una OT en cola; la toma es atómica (dos personas no pueden tomar la misma) | P0 | FR-013 | R1 (D3) |
| FR-015 | Un responsable de la especialidad puede **asignar** una OT de su cola a un miembro de su equipo | P0 | FR-009, FR-013 | R1 (D3) · R2 (D5) |
| FR-016 | El ejecutor de una OT "Asignada" la **verifica**: si le corresponde pasa a "En progreso"; si no, la OT queda **sin ejecutor en su especialidad en estado "Devuelta"** con motivo obligatorio, a la espera del responsable | P0 | FR-014 | CLAUDE §1 paso 11 · AUD-N3 · R4/R5 (D13, D15) |
| FR-017 | El **responsable de la especialidad actual o el Administrador** reasigna una OT abierta a **otra especialidad**: cambia `especialidad`, queda en la cola destino ("Pendiente", sin ejecutor) y exige motivo. Para una OT "Devuelta", el responsable puede en cambio asignarla a otro miembro (FR-015) | P0 | FR-013 | CLAUDE §1 paso 12 · AUD-N2 · R4/R5 (D14, D15) |
| FR-018 | Cada evento de asignación (encolar, tomar, asignar, devolver o reasignar) queda registrado con actor, especialidad origen y destino, usuario origen y destino, motivo y fecha | P0 | FR-013 | AUD-N2 · R3 (D8, calidad del ruteo) |
| FR-019 | Solo el ejecutor asignado puede cerrar la OT; el cierre finaliza la Solicitud o RQ de origen (se conserva el comportamiento actual) | P0 | FR-014 | CLAUDE §1 paso 14 · Código (`OrdenServiceImpl.cerrarOrden`) |
| FR-020 | Cada miembro ve: (a) la cola de sus especialidades, (b) sus OT asignadas. Cada responsable ve además las OT del equipo con su carga | P0 | FR-013, FR-015 | R1 (D3) · R3 (D9) |

### E4 — Ciclo de vida del Requerimiento

| ID | Requerimiento | Prioridad | Depende de | Fuente |
|---|---|---|---|---|
| FR-021 | Rechazar un RQ originado en una Solicitud cambia esa Solicitud a "Rechazado" y registra el motivo en su historial | P0 | — | R2 (D6) · AUD-N4 |
| FR-022 | El Cliente ve el motivo del rechazo en el seguimiento de su Solicitud | P0 | FR-021 | R2 (D6) · CLAUDE §1 paso 2 |
| FR-023 | Aprobar un RQ abre un paso de confirmación con la especialidad destino (preseleccionada con la del RQ). Al confirmarlo se aprueba y se genera la OT en cola en la misma transacción | P0 | FR-013 | R2 (D7) |
| FR-024 | Si la aprobación se registra sin completar la generación de la OT, el RQ queda "Aprobado", la Solicitud sigue "En revisión" y la OT se puede generar luego de forma manual | P0 | FR-023 | R2 (D7) |
| FR-025 | El campo `aprobado` de la aprobación es obligatorio (no se acepta nulo) | P1 | — | AUD-B4 |

### E5 — Notificaciones en tiempo real

| ID | Requerimiento | Prioridad | Depende de | Fuente |
|---|---|---|---|---|
| FR-026 | Los miembros de una especialidad reciben un aviso en tiempo real cuando entra una OT a su cola (nueva o reasignada) | P0 | FR-013, FR-017 | R3 (D11) |
| FR-027 | El usuario recibe un aviso en tiempo real cuando un responsable le asigna una OT | P0 | FR-015 | R3 (D11) |
| FR-028 | El menú muestra un contador de OT en cola y de OT asignadas pendientes, actualizado por los eventos | P1 | FR-026 | R3 (D11) |
| FR-029 | Si se pierde la conexión, el cliente reconecta y resincroniza los contadores consultando la API | P1 | FR-026 | R3 (D11) |

### E6 — KPIs y dashboards por rol

| ID | Requerimiento | Prioridad | Depende de | Fuente |
|---|---|---|---|---|
| FR-030 | KPI de SLA de despacho: % de Solicitudes despachadas antes de `fecha_limite_despacho`, por prioridad | P0 | — | R3 (D8) · Código (`SlaCalculator`) |
| FR-031 | KPI de tiempos de ciclo: Solicitud→despacho, OT en cola→tomada, tomada→cerrada y punta a punta, por especialidad (mediana y p90) | P0 | FR-018 | R3 (D8) |
| FR-032 | KPI de cola y carga: OT en cola por especialidad, antigüedad de la OT más vieja y OT abiertas por persona | P0 | FR-013 | R3 (D8) |
| FR-033 | KPI de calidad del ruteo: tasa de reasignación entre especialidades y tasa de aprobación/rechazo de RQ | P0 | FR-018, FR-021 | R3 (D8) |
| FR-034 | Dashboard por rol: Cliente (sus solicitudes), Despachador (bandeja por despachar y SLA), Operaciones (mi cola y mis OT), Responsable (cola y carga del equipo), Admin (RQ por aprobar y KPIs globales) | P0 | FR-030 a FR-033 | R3 (D9) |
| FR-035 | Todos los KPIs aceptan un rango de fechas (hoy, semana, mes o personalizado) | P0 | FR-030 | R3 (D10) |
| FR-036 | Los KPIs se pueden exportar a CSV respetando el rango y la visibilidad del usuario | P1 | FR-035 | R3 (D10) |
| FR-037 | Los KPIs respetan la autorización por recurso: un responsable solo ve sus especialidades y el Cliente nunca ve KPIs globales | P0 | FR-006 | CLAUDE §6.5 · AUD-S4 |

### E7 — Integridad y deuda técnica

| ID | Requerimiento | Prioridad | Depende de | Fuente |
|---|---|---|---|---|
| FR-038 | Los números ST-/RQ-/OT- se generan con secuencias de BD: únicos con concurrencia y sin reutilizarse tras borrados | P1 | — | AUD-B2 |
| FR-039 | Solicitudes, RQ y OT con historial no se eliminan físicamente; el intento devuelve 409 con un mensaje claro | P1 | — | AUD-B3 |
| FR-040 | Se mantiene `ddl-auto=update` (D18); los objetos que Hibernate no crea (secuencias de numeración, índice parcial de cola) se crean con un script SQL idempotente (`IF NOT EXISTS`) ejecutado al arrancar | P1 | — | AUD-B9 · R5 (D18) |
| FR-041 | Las FK obligatorias inexistentes responden 404, no 500 | P2 | — | AUD-B5 |
| FR-042 | Los adjuntos de OT solo se gestionan por el fileserver; se elimina `url_adjunto` del body de generar OT | P2 | — | AUD-B6 |

### E8 — Experiencia frontend del flujo

| ID | Requerimiento | Prioridad | Depende de | Fuente |
|---|---|---|---|---|
| FR-043 | Bandeja del Despachador con Solicitudes "Pendiente" ordenadas por vencimiento de SLA; al despachar (OT o RQ) se confirma o cambia la especialidad entre las 5, precargada con la del activo (sin elegir persona) | P0 | FR-013 | R3 (D9) · `src/frontend/README.md` ("seleccionar la especialidad, no mostrar nombre") |
| FR-044 | Vista de Operaciones con las pestañas "Cola de mis especialidades" y "Mis OT", con las acciones Tomar, Confirmar, No me corresponde (reasignar) y Cerrar | P0 | FR-014, FR-016, FR-017 | R1 (D3) |
| FR-045 | Vista del Responsable: cola y carga por miembro, con la acción Asignar | P0 | FR-015 | R2 (D5) |
| FR-046 | Modal de aprobación de RQ con la especialidad destino (D7) | P0 | FR-023 | R2 (D7) |
| FR-047 | Timeline de seguimiento para el Cliente (estados, OT asociada y motivo de rechazo) | P1 | FR-022 | CLAUDE §1 paso 2 |
| FR-048 | Pantalla de administración de miembros y responsables por especialidad | P1 | FR-011 | R2 (D5) |

## 2. Requerimientos no funcionales

| ID | Requerimiento | Medición | Fuente |
|---|---|---|---|
| NFR-001 | Todo endpoint nuevo exige JWT y un permiso declarado con `@PreAuthorize`; ninguno es público | Revisión de código y test de seguridad por endpoint | CLAUDE §6 |
| NFR-002 | Todo endpoint nuevo pasa por el `RateLimitFilter` centralizado; una conexión SSE cuenta como 1 solicitud al abrirse | Test de integración 429 | CLAUDE §6.6 |
| NFR-003 | El aviso SSE llega en menos de 5 s (p95) desde el commit de la transacción | Test de carga con 50 conexiones simultáneas | R3 (D11) |
| NFR-004 | Los endpoints de KPI responden en menos de 1,5 s (p95) para un rango de 90 días con 50 000 OT | Test de carga con datos sintéticos | R3 (D10) |
| NFR-005 | La toma de una OT es atómica: con 2 tomas concurrentes, exactamente 1 tiene éxito y la otra recibe 409 | Test de concurrencia | FR-014 |
| NFR-006 | El token JWT nunca viaja en la URL (incluido SSE) | Revisión de código y logs de acceso | CLAUDE §6.1 |
| NFR-007 | El CSV exportado no incluye contraseñas, hashes ni tokens; los emails solo aparecen para Admin | Test de contenido del CSV | CLAUDE §6.1 |
| NFR-008 | El registro SSE en memoria limpia las conexiones cerradas y envía un heartbeat cada 25 s (por debajo del timeout típico de los proxies) | Test de integración | E5 |
| NFR-009 | El seeder y el script SQL de arranque son idempotentes: arrancar N veces no duplica datos ni falla | Test de integración con 2 arranques consecutivos | FR-012, FR-040 |
| NFR-010 | Cobertura mínima de servicios del flujo (Solicitud, RQ, Aprobación, OT, Cola): 80 % de líneas y 70 % de ramas | JaCoCo en CI | — |
| NFR-011 | Las nuevas pantallas cumplen WCAG 2.1 AA (contraste, foco y teclado) y funcionan a 360 px de ancho | Auditoría con `impeccable audit` | CLAUDE §8 |

## 3. Sugerencias no solicitadas

> Fuera de la tabla principal porque no salen del pedido ni de las rondas de clarificación. Se incluyen solo si se aprueban.

| ID | Sugerencia | Motivo |
|---|---|---|
| [SUGGESTED] SG-01 | Escalado automático al responsable si una OT supera N horas en cola | Previene la inanición de la cola; enlazado a OQ-04 |
| [SUGGESTED] SG-02 | Mover el JWT de `localStorage` a memoria más una cookie httpOnly de refresh | Reduce el impacto de un XSS; hoy el token vive en `localStorage` (`core/auth/auth.service.ts:59`) |
| [SUGGESTED] SG-03 | Purga periódica del mapa de ventanas del rate limit | AUD-S9 (crecimiento sin límite) |
| [SUGGESTED] SG-04 | Restringir Swagger por perfil (`dev` únicamente) | AUD-S8 |

## 4. Estado de implementación

> Se actualiza al cerrar cada etapa (regla del 2026-10-03). ✅ hecho y probado · 🟡 parcial · ⏳ pendiente.

| Etapa | FR | Estado | Evidencia |
|---|---|---|---|
| 1 (E1+E7) | FR-001, FR-002, FR-003, FR-004, FR-005, FR-007 | ✅ 2026-10-03 | `FlujoNegocioIntegrationTest` fase 7 + suite completa 37/37 |
| 1 (E1+E7) | FR-006 | 🟡 | Visibilidad por permisos de alcance; el chequeo de rol "Operaciones" al elegir ejecutor se elimina en la etapa 3 |
| 1 (E1+E7) | FR-025, FR-038, FR-039, FR-041, FR-042 | ✅ 2026-10-03 | `AprobacionRequest @NotNull`, `NumeracionService` + `db/init.sql`, borrado → 409, FK → 404, sin `url_adjunto` |
| 1 (E1+E7) | FR-040 | ✅ 2026-10-03 | `spring.sql.init` + `db/init.sql` idempotente (D18) |
| 2 (E2) | FR-010 | ✅ 2026-10-03 | Catálogo de 5 especialidades en el seeder |
| 2 (E2) | FR-008, FR-009, FR-011 | ✅ 2026-10-03 (API) | `EquipoEspecialidadController`; fase 8 de `FlujoNegocioIntegrationTest`. La pantalla (FR-048) va en la etapa 5 |
| 2 (E2) | FR-012 | ✅ 2026-10-04 | Seed de equipos (D24), activos con especialidades (D25) y usuarios nuevos; equipos verificados en BD tras correr la suite |
| 3 (E3+E4) | FR-013, FR-014, FR-015, FR-016, FR-017, FR-018, FR-019, FR-020, FR-021, FR-023, FR-024 | ✅ 2026-10-03 (API) | `ColaOrdenService`, `GeneradorOrdenService`, `AprobacionServiceImpl`; fases 2–7 y 10 de `FlujoNegocioIntegrationTest` |
| 3 (E3+E4) | FR-022 | ✅ 2026-10-03 | Backend probado (fase 7); el detalle de la Solicitud muestra el estado "Rechazado" y el historial con el motivo |
| 4 (E5) | FR-026, FR-027 | ✅ 2026-10-03 (backend) | `NotificacionSseIntegrationTest` (aviso < 5 s al miembro, no al ajeno) |
| 4 (E5) | FR-028, FR-029 | 🟡 2026-10-03 | `NotificacionStreamService` (reconexión con backoff, resincronización cada 60 s) y contador en el menú. Compila; sin prueba automatizada de frontend ni verificación en navegador |
| 5 (E8) | FR-043, FR-044, FR-045, FR-046, FR-047, FR-048 | 🟡 2026-10-03 | Implementado y compilando (`ng build` OK); la API que consumen está probada. Falta prueba e2e/visual en navegador |
| 6 (E6) | FR-030, FR-031, FR-032, FR-033, FR-035, FR-036, FR-037 | ✅ 2026-10-03 (API) | `KpiIntegrationTest` (4), `EstadisticaYConteoTest` (6), `CsvExporterTest` (2) |
| 6 (E6) | FR-034 | 🟡 2026-10-03 | Dashboard por permisos: "Mi trabajo" (ejecutor), panel de KPIs (`kpi.read`, alcance según rol), resúmenes existentes recortados por visibilidad. Compila; falta verificación visual |
| 7 | FR-049, FR-050, FR-051, FR-052 | ✅ 2026-10-04 (API) / 🟡 UI | `ActivoServiceImpl` + `activo_especialidad`; `EscaladoColaIntegrationTest`, `UmbralesEscaladoTest`; `MetasKpiTest`, `KpiIntegrationTest` (6 familias). UI compila; falta verificación visual |

## 5. Requerimientos agregados el 2026-10-04 (ronda 8–9)

| ID | Requerimiento | Prioridad | Depende de | Fuente |
|---|---|---|---|---|
| FR-049 | Un activo pertenece a una o más especialidades, con una principal; la Solicitud nace con la principal y el despacho muestra primero las del activo | P0 | FR-002 | R8 · R9 (D25) |
| FR-050 | Una OT en cola sin tomar se escala por prioridad (Alta 30 min, Media 2 h, Baja 4 h) a los responsables y, al doble, al Administrador, con aviso en tiempo real y lista en el dashboard | P0 | FR-013, FR-026 | R8 · R9 (D26) |
| FR-051 | SLA de atención por prioridad (Alta 8 h, Media 24 h, Baja 72 h, solo bajo contrato) y su KPI de cumplimiento; KPI de tiempo de decisión de RQ (≤ 48 h) | P0 | FR-030 | R8 · R9 (D27) |
| FR-052 | Cada indicador con meta muestra la meta y si cumple, está en alerta o no cumple; metas configurables por variables de entorno | P1 | FR-034 | R8 · R9 (D27) |
