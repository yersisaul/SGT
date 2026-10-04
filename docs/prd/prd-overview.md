# PRD — SGT: Flujo de OT por especialidad, seguridad y tablero de KPIs

| Campo | Valor |
|---|---|
| Tipo de PRD | Feature, modo **visión completa** (épicas con tallas S/M/L/XL; sin DDL ni sprints detallados) |
| Fecha | 2026-10-03 |
| Autor | Equipo SGT (generado con asistencia de IA) |
| Fuentes | CLAUDE.md §1 (flujo de 14 pasos) y §6 (seguridad) · `docs/AUDITORIA.md` · análisis del código (backend Spring Boot y frontend Angular 21) · 3 rondas de clarificación con el dueño del producto |
| Estado | Borrador para revisión |

---

## 1. Resumen ejecutivo

El SGT ya cubre el ciclo Solicitud → (OT | RQ → Aprobación → OT) → Cierre. Sin embargo, la auditoría encontró que **el corazón operativo del nuevo flujo de negocio no existe todavía**:

- las OT se asignan a mano a una persona, no a una **especialidad** (pasos 5 y 9);
- la reasignación mueve la OT entre personas, nunca entre especialidades (paso 12);
- no existe el paso "¿me corresponde?" (paso 11);
- un RQ rechazado deja la Solicitud del cliente en "En revisión" para siempre (paso 8);
- el Cliente puede ver todas las OT y editar solicitudes ajenas (§6.5).

Este PRD define **8 épicas** que llevan el sistema al flujo definitivo. El eje es una **cola de trabajo por especialidad**: cualquier miembro toma la OT o el responsable de la especialidad la asigna. Sobre esa cola se montan las **notificaciones en tiempo real (SSE)** y un **tablero de KPIs por rol** con rango de fechas y exportación a CSV.

## 2. Problema

| Actor | Dolor actual | Evidencia |
|---|---|---|
| Despachador / Admin | Al generar la OT debe elegir a mano a una persona de Operaciones, sin saber quién pertenece a qué especialidad ni su carga | `SolicitudServiceImpl.resolverEjecutorOperaciones`, `Usuario` sin relación con `Especialidad` |
| Operaciones | Si la OT no le corresponde, solo puede pasarla a otra persona; la especialidad de la OT queda incorrecta | `OrdenServiceImpl.reasignarOrden` no cambia `orden.especialidad` |
| Cliente | Su solicitud queda "En revisión" indefinidamente cuando el RQ se rechaza; no recibe el motivo | `AprobacionServiceImpl.crearAprobacion` no toca la Solicitud |
| Negocio | No hay medición de SLA, tiempos de ciclo ni calidad del ruteo | El dashboard actual solo muestra conteos por estado |
| Seguridad | Fugas de acceso del rol Cliente | AUDITORIA S1, S2, S3, N7 |

## 3. Objetivos y métricas de éxito

> Línea base: el sistema **no mide** hoy estos indicadores. Las líneas base se calcularán en la épica E6 a partir de `historial_solicitud`, `historial_requerimiento` e `historial_orden` (todas con `fecha`), sobre los últimos 90 días. Los valores objetivo son **propuestas para validar** con el negocio (ver OQ-07).

| ID | Objetivo | Línea base | Objetivo propuesto | Fuente de la línea base |
|---|---|---|---|---|
| G1 | Toda OT nace asignada a una especialidad y llega a su cola sin intervención manual | 0 % (asignación manual a persona) | 100 % | Código: `generarOrdenDesde*` exige `id_usuario_ejecutor` |
| G2 | Cumplimiento del SLA de despacho (Solicitud despachada antes de `fecha_limite_despacho`) | No medido | ≥ 90 % mensual, alerta < 80 % (D27) | `SlaCalculator` + historial_solicitud |
| G2b | Cumplimiento del SLA de atención (registro → cierre de OT, bajo contrato) | No medido | ≥ 85 %, luego 90 % (D27) | KPI `sla-atencion` |
| G3 | Tiempo de OT en cola sin tomar | No medido (el concepto no existe) | Mediana ≤ 1 h y p90 ≤ 4 h, horas calendario (D27) | `asignacion_orden` (E3) |
| G4 | Tasa de reasignación entre especialidades (calidad del ruteo) | No medible (la reasignación no cambia especialidad) | ≤ 10 % de OT; devoluciones ≤ 15 % (D27) | `asignacion_orden` (E3) |
| G5 | Solicitudes "huérfanas" (RQ rechazado y Solicitud en "En revisión") | Desconocido en BD; 100 % de los rechazos lo producen por diseño | 0 | AUDITORIA N4 |
| G6 | Hallazgos de seguridad críticos y altos de la auditoría | 4 críticos (B1, S1, S2, N1/N2) + 5 altos | 0 abiertos | `docs/AUDITORIA.md` |
| G7 | Latencia de aviso de nueva OT al miembro de la especialidad | N/A (sin avisos) | < 5 s desde la creación | SSE (E5) |

## 4. Alcance

### Dentro del alcance (épicas)

| Épica | Nombre | Talla | Pasos del flujo |
|---|---|---|---|
| E1 | Correcciones críticas y seguridad por recurso | M | 1, 2 · §6.5 |
| E2 | Especialidades y equipos (N:M + responsable) | M | 5, 9, 12 |
| E3 | Cola de OT por especialidad (tomar, asignar, verificar, reasignar) | L | 5, 9, 10, 11, 12, 13 |
| E4 | Ciclo de vida del Requerimiento (rechazo y aprobación con generación de OT) | M | 7, 8, 9 |
| E5 | Notificaciones en tiempo real (SSE) | M | 10, 12 |
| E6 | KPIs y dashboards por rol (rango de fechas y CSV) | L | Transversal |
| E7 | Integridad de datos y deuda técnica | M | Transversal |
| E8 | Experiencia frontend del flujo (bandejas, modal, timeline) | L | 2, 3, 8, 10, 11, 12 |

### Fuera del alcance

- Gestión de contratos (catálogo de servicios bajo contrato por cliente): hoy el Despachador decide "bajo contrato" por criterio propio. Ver OQ-08.
- Notificaciones por correo electrónico o push móvil.
- Despliegue con varias instancias (el registro SSE y el rate limit son en memoria y suponen una sola instancia).
- Reportes programados o enviados por correo.

## 5. Decisiones del producto (clarificación)

| # | Decisión | Ronda |
|---|---|---|
| D1 | Visión completa (todas las épicas) | R1 |
| D2 | Usuario ↔ Especialidad **N:M** (tabla `usuario_especialidad`) | R1 |
| D3 | **Cola de especialidad**: cualquier miembro toma la OT y el **responsable** puede asignarla a un miembro de su equipo | R1 |
| D4 | Catálogo: Desarrollo, Implementación, DevOPS, **Soporte y mantenimiento de código**, **Soporte de infraestructura y configuración de analíticas** | R1 |
| D5 | Responsable = flag `es_responsable` en `usuario_especialidad` (uno o varios por especialidad) más el permiso `orden.asignar` | R2 |
| D6 | RQ rechazado → la Solicitud de origen pasa a **"Rechazado"**, con el motivo visible para el Cliente | R2 |
| D7 | RQ aprobado → **modal** para que el Admin elija la especialidad, y la OT se genera automáticamente. Si no completa el modal: RQ "Aprobado", Solicitud "En revisión" y la OT se genera manualmente más tarde | R2 |
| D8 | KPIs: SLA de despacho, tiempos de ciclo, cola y carga, calidad del ruteo | R3 |
| D9 | Dashboard **distinto por rol** (Cliente, Despachador, Operaciones, Responsable, Admin) | R3 |
| D10 | Rango de fechas y exportación a CSV | R3 |
| D11 | Avisos en **tiempo real con SSE** | R3 |
| D12 | **Aprobados** los 6 permisos nuevos y los cambios de BD de `prd-technical.md` §2 | R4 |
| D13 | Estados de OT nuevos **"Asignada"** (tiene ejecutor, sin confirmar) y **"Devuelta"** (el ejecutor dijo "no me corresponde"; espera decisión del responsable). Criterio delegado a Claude | R4 |
| D14 | Reasignan a otra especialidad: el **responsable de la especialidad actual** y el **Administrador** | R4 · R5 |
| D15 | "No me corresponde" → la OT queda **sin ejecutor en su especialidad, en estado "Devuelta" con el motivo**; el responsable la reasigna a otra especialidad o la asigna a otro miembro | R5 |
| D16 | Despachador (al despachar la ST) y Admin (al aprobar el RQ) eligen entre las **5 especialidades**, incluidas las 2 de Soporte | R4 |
| D17 | Si el Admin cancela el modal: la **aprobación se registra igual**, el RQ queda "Aprobado", la Solicitud sigue "En revisión" y la OT se genera manualmente después | R4 |
| D18 | **Sin Flyway**: se mantiene `ddl-auto=update`; las secuencias y el índice parcial se crean con un script SQL idempotente | R5 |
| D19 | La BD se limpió: el `DataSeeder` puede reescribirse. Las asignaciones de equipos y la especialidad de cada activo las aportará el dueño del producto (no se inventan) | R4 · R5 |
| D20 | Visibilidad por **permisos de alcance** `solicitud.read_all`, `requerimiento.read_all`, `orden.read_all` (Despachador y Admin). Sin ellos: lo propio, y en Operaciones lo de sus especialidades o asignado | R6 |
| D21 | `.env` fuera del repositorio (`.env.example` como plantilla); la rotación de secretos queda a cargo del dueño del producto | R6 |
| D22 | La especialidad de una Solicitud se **toma del activo** al registrarla (el Cliente no la elige); el Despachador la confirma o cambia entre las 5 al despachar | R7 |
| D23 | Las pruebas de integración se ejecutan contra la BD de desarrollo del `.env` | R7 |
| D24 | Equipos: Desarrollo y Soporte y mant. de código = yortiz (resp.), lulloa, aperalta · Implementación = pgaspar (resp.), ddiaz, mjimenez · DevOPS = pgaspar (resp.), ddiaz · Soporte de infraestructura = pgaspar (resp.), mjimenez, rjuarez, olopez (Oscar Lopez), ldesposorio (Cristofer Geronimo) | R8 |
| D25 | Activo ↔ Especialidad **N:M con principal** (`activo_especialidad`; la Solicitud nace con la principal). Azor Panel y Azor Analytics → Soporte y mant. de código (+ Desarrollo, DevOPS); Network Optix → Soporte de infraestructura (+ Implementación); hardware → Soporte de infraestructura | R8 · R9 |
| D26 | **Escalado** de OT en cola sin tomar por prioridad: Alta 30 min, Media 2 h, Baja 4 h → responsables; al doble → Administrador (`.env`) | R8 · R9 |
| D27 | **Metas de KPI aprobadas**: SLA despacho ≥ 90 % (alerta < 80 %); SLA de atención (nuevo: Alta 8 h, Media 24 h, Baja 72 h) ≥ 85 %; cola mediana ≤ 1 h y p90 ≤ 4 h; decisión de RQ ≥ 90 % en ≤ 48 h; reasignación ≤ 10 %; devoluciones ≤ 15 %; carga ≤ 5 OT por persona; aprobación de RQ sin meta. Horas calendario; recalibrar a los 60 días | R8 · R9 |
| D28 | El Despachador **no** rechaza Solicitudes directamente: solo se rechazan vía RQ rechazado | R8 |

## 6. Restricciones (CLAUDE.md)

- Cambios de BD (tabla `usuario_especialidad`, `orden.id_usuario` nullable, `asignacion_orden`, `orden.version`, secuencias): **aprobados** el 2026-10-03 (D12, CLAUDE §3.3).
- Permisos nuevos (`orden.tomar`, `orden.asignar`, `orden.verificar`, `kpi.read`, `kpi.export`, `especialidad.gestionar_equipo`): **autorizados** el 2026-10-03 (D12, CLAUDE §6.2).
- Se mantiene la arquitectura por capas Controller → Service → Repository. Las reglas de autorización por recurso van en un componente dedicado (§6.4/6.5).

## 7. Preguntas abiertas

| ID | Pregunta | Estado | Impacta |
|---|---|---|---|
| OQ-01 | Aprobar los 6 permisos nuevos propuestos | ✅ Cerrada (D12) | E3, E6 |
| OQ-02 | ¿Estado nuevo "Asignada" para la OT? | ✅ Cerrada (D13): se agregan "Asignada" y "Devuelta" | E3, E6 |
| OQ-03 | ¿Quién reasigna entre especialidades? | ✅ Cerrada (D14, D15) | E3 |
| OQ-04 | ¿La cola tiene un tiempo máximo antes de escalar al responsable? | ✅ Cerrada (D26) | E3, E5 |
| OQ-05 | ¿El Despachador puede editar una Solicitud ya despachada? | Abierta: se aplica FR-007 (no se cambian especialidad ni activo tras despachar) salvo indicación en contrario | E1 |
| OQ-06 | Destino de las OT de "Soporte" y personas sin especialidad | ✅ Cerrada (D16, D19): BD limpia, sin OT que migrar; los equipos los define el dueño del producto | E2 |
| OQ-07 | Validar los valores objetivo de G2 a G4 | ✅ Cerrada (D27) | E6 |
| OQ-08 | ¿Se modelará el catálogo de contratos para decidir "bajo contrato" de forma asistida? | Abierta (futuro) | Futuro |
| OQ-09 | ¿El Despachador puede rechazar directamente una Solicitud sin crear RQ? | ✅ Cerrada (D28): no | E4 |
| OQ-10 | Lista real de miembros y responsables por especialidad, y especialidad de cada activo sembrado | ✅ Cerrada (D24, D25) | E2 |
