# JIRA — SGT (visión completa)

| Campo | Valor |
|---|---|
| Proyecto | SGT |
| Fecha | 2026-10-03 |
| Estimación | Tallas (S/M/L). **Sin story points**: en modo visión completa se asignan al detallar cada épica |
| Duración estimada | 25–36 semanas para 1 persona (ver `prd-roadmap.md`) |
| AC | Se referencian los IDs del PRD (`prd-acceptance.md`); este archivo no crea numeración propia |

## Épicas

| Clave | Épica | Talla | Historias |
|---|---|---|---|
| SGT-E1 | Correcciones críticas y seguridad por recurso | M | SGT-1 a SGT-5 |
| SGT-E2 | Especialidades y equipos | M | SGT-6, SGT-7 |
| SGT-E3 | Cola de OT por especialidad | L | SGT-8 a SGT-14 |
| SGT-E4 | Ciclo de vida del Requerimiento | M | SGT-15, SGT-16 |
| SGT-E5 | Notificaciones SSE | M | SGT-17 |
| SGT-E6 | KPIs y dashboards por rol | L | SGT-18 a SGT-20 |
| SGT-E7 | Integridad y deuda técnica | M | SGT-21, SGT-22 |
| SGT-E8 | Frontend del flujo | L | SGT-23 a SGT-28 |

## Historias

| Clave | Historia (US) | Tipo | Prioridad | Talla | Épica | AC | Depende de | Etiquetas |
|---|---|---|---|---|---|---|---|---|
| SGT-1 | US-01 Seeder arranca en BD limpia | Bug | Highest | S | E1 | AC-001 | — | backend, seed |
| SGT-2 | US-02 Solicitud nace Pendiente | Bug | Highest | S | E1 | AC-002 | — | backend, seguridad |
| SGT-3 | US-03 Aislamiento del Cliente (editar y ver OT) | Bug | Highest | M | E1 | AC-003, AC-004 | — | backend, seguridad |
| SGT-4 | US-04 Autorización por recurso centralizada | Story | High | M | E1 | AC-005, AC-006 | SGT-3 | backend, seguridad |
| SGT-5 | US-05 Bloquear la edición de Solicitudes despachadas | Story | Medium | S | E1 | AC-007 | — | backend |
| SGT-6 | US-06 Usuario ↔ Especialidad N:M con responsable | Story | Highest | M | E2 | AC-008, AC-009, AC-011 | SGT-21 | backend, bd |
| SGT-7 | US-07 Catálogo de 5 especialidades y migración | Story | Highest | M | E2 | AC-010, AC-012 | SGT-6 | backend, bd, migración |
| SGT-8 | US-08 Generar OT en la cola de la especialidad | Story | Highest | M | E3 | AC-013 | SGT-6 | backend |
| SGT-9 | US-09 Tomar OT (atómico) | Story | Highest | M | E3 | AC-014 | SGT-8 | backend, concurrencia |
| SGT-10 | US-10 Responsable asigna OT | Story | Highest | M | E3 | AC-015 | SGT-8 | backend |
| SGT-11 | US-11 Verificar si corresponde | Story | High | S | E3 | AC-016 | SGT-9 | backend |
| SGT-12 | US-12 Reasignar a otra especialidad con historial | Story | Highest | L | E3 | AC-017, AC-018 | SGT-8 | backend, bd |
| SGT-13 | US-13 Cierre solo por el ejecutor | Story | High | S | E3 | AC-019 | SGT-9 | backend, seguridad |
| SGT-14 | US-14 Vistas de cola, mis OT y equipo (API) | Story | High | M | E3 | AC-020 | SGT-9, SGT-10 | backend |
| SGT-15 | US-15 RQ rechazado → Solicitud Rechazada | Bug | Highest | M | E4 | AC-021, AC-022 | — | backend |
| SGT-16 | US-16 Aprobar RQ y generar la OT en un paso | Story | Highest | M | E4 | AC-023, AC-024, AC-025 | SGT-8 | backend |
| SGT-17 | US-17 Avisos SSE de cola y asignación | Story | High | L | E5 | AC-026, AC-027, AC-028, AC-029 | SGT-8, SGT-10 | backend, frontend, tiempo-real |
| SGT-18 | US-18 KPI de SLA y tiempos de ciclo | Story | High | L | E6 | AC-030, AC-031 | SGT-12 | backend, kpi |
| SGT-19 | US-19 KPI de cola, carga y ruteo | Story | High | M | E6 | AC-032, AC-033 | SGT-12, SGT-15 | backend, kpi |
| SGT-20 | US-20 Dashboards por rol, rango y CSV | Story | High | L | E6 | AC-034, AC-035, AC-036, AC-037 | SGT-18, SGT-19 | frontend, kpi |
| SGT-21 | US-21 Secuencias, borrado seguro y script SQL de arranque | Story | High | M | E7 | AC-038, AC-039, AC-040 | — | backend, bd |
| SGT-22 | US-22 404 coherentes y adjuntos solo por el fileserver | Bug | Medium | S | E7 | AC-041, AC-042 | — | backend |
| SGT-23 | US-23 Bandeja del Despachador por SLA | Story | High | M | E8 | AC-043 | SGT-8 | frontend |
| SGT-24 | US-24 Vista de Operaciones: Cola y Mis OT | Story | Highest | L | E8 | AC-044 | SGT-9, SGT-11, SGT-12 | frontend |
| SGT-25 | US-25 Vista de Equipo del responsable | Story | High | M | E8 | AC-045 | SGT-10 | frontend |
| SGT-26 | US-26 Modal de aprobación de RQ | Story | Highest | S | E8 | AC-046 | SGT-16 | frontend |
| SGT-27 | US-27 Timeline del Cliente | Story | Medium | M | E8 | AC-047 | SGT-15 | frontend |
| SGT-28 | US-28 Administración de equipos | Story | Medium | S | E8 | AC-048 | SGT-6 | frontend |

**Total: 28 historias (8 S · 15 M · 5 L), 8 épicas.** Recuento verificado contra `prd-user-stories.md`.

## Subtareas tipo (aplican a cada historia de backend)

1. DTO request/response con Bean Validation.
2. Método de servicio y regla en `AutorizacionRecursoService`.
3. Endpoint con `@PreAuthorize` y permiso aprobado (OQ-01).
4. Si hay objetos que Hibernate no crea, agregarlos a `db/init.sql` con `IF NOT EXISTS`.
5. Pruebas unitarias e integración (ver `prd-tests.md`).
6. Actualizar el permiso en la matriz del `DataSeeder`.

## CSV para importar

```csv
Issue Type,Summary,Priority,Labels,Epic Link,Description
Bug,"US-01 Seeder arranca en BD limpia",Highest,"backend seed",SGT-E1,"AC-001. Talla S"
Bug,"US-02 Solicitud nace Pendiente",Highest,"backend seguridad",SGT-E1,"AC-002. Talla S"
Bug,"US-03 Aislamiento del Cliente",Highest,"backend seguridad",SGT-E1,"AC-003 AC-004. Talla M"
Story,"US-04 Autorización por recurso centralizada",High,"backend seguridad",SGT-E1,"AC-005 AC-006. Talla M. Depende SGT-3"
Story,"US-05 Bloquear edición de Solicitud despachada",Medium,backend,SGT-E1,"AC-007. Talla S"
Story,"US-06 Usuario-Especialidad N:M con responsable",Highest,"backend bd",SGT-E2,"AC-008 AC-009 AC-011. Talla M. Depende SGT-21"
Story,"US-07 Catálogo de 5 especialidades y migración",Highest,"backend bd migracion",SGT-E2,"AC-010 AC-012. Talla M. Depende SGT-6"
Story,"US-08 Generar OT en cola de especialidad",Highest,backend,SGT-E3,"AC-013. Talla M. Depende SGT-6"
Story,"US-09 Tomar OT atómico",Highest,"backend concurrencia",SGT-E3,"AC-014. Talla M. Depende SGT-8"
Story,"US-10 Responsable asigna OT",Highest,backend,SGT-E3,"AC-015. Talla M. Depende SGT-8"
Story,"US-11 Verificar si corresponde",High,backend,SGT-E3,"AC-016. Talla S. Depende SGT-9"
Story,"US-12 Reasignar a otra especialidad",Highest,"backend bd",SGT-E3,"AC-017 AC-018. Talla L. Depende SGT-8"
Story,"US-13 Cierre solo por el ejecutor",High,"backend seguridad",SGT-E3,"AC-019. Talla S. Depende SGT-9"
Story,"US-14 Vistas de cola, mis OT y equipo (API)",High,backend,SGT-E3,"AC-020. Talla M. Depende SGT-9 SGT-10"
Bug,"US-15 RQ rechazado -> Solicitud Rechazada",Highest,backend,SGT-E4,"AC-021 AC-022. Talla M"
Story,"US-16 Aprobar RQ y generar OT en un paso",Highest,backend,SGT-E4,"AC-023 AC-024 AC-025. Talla M. Depende SGT-8"
Story,"US-17 Avisos SSE",High,"backend frontend tiempo-real",SGT-E5,"AC-026 AC-027 AC-028 AC-029. Talla L. Depende SGT-8 SGT-10"
Story,"US-18 KPI SLA y tiempos de ciclo",High,"backend kpi",SGT-E6,"AC-030 AC-031. Talla L. Depende SGT-12"
Story,"US-19 KPI cola, carga y ruteo",High,"backend kpi",SGT-E6,"AC-032 AC-033. Talla M. Depende SGT-12 SGT-15"
Story,"US-20 Dashboards por rol, rango y CSV",High,"frontend kpi",SGT-E6,"AC-034 AC-035 AC-036 AC-037. Talla L. Depende SGT-18 SGT-19"
Story,"US-21 Secuencias, borrado seguro y script SQL de arranque",High,"backend bd",SGT-E7,"AC-038 AC-039 AC-040. Talla M"
Bug,"US-22 404 coherentes y adjuntos por fileserver",Medium,backend,SGT-E7,"AC-041 AC-042. Talla S"
Story,"US-23 Bandeja del Despachador por SLA",High,frontend,SGT-E8,"AC-043. Talla M. Depende SGT-8"
Story,"US-24 Vista Operaciones: Cola y Mis OT",Highest,frontend,SGT-E8,"AC-044. Talla L. Depende SGT-9 SGT-11 SGT-12"
Story,"US-25 Vista de Equipo del responsable",High,frontend,SGT-E8,"AC-045. Talla M. Depende SGT-10"
Story,"US-26 Modal de aprobación de RQ",Highest,frontend,SGT-E8,"AC-046. Talla S. Depende SGT-16"
Story,"US-27 Timeline del Cliente",Medium,frontend,SGT-E8,"AC-047. Talla M. Depende SGT-15"
Story,"US-28 Administración de equipos",Medium,frontend,SGT-E8,"AC-048. Talla S. Depende SGT-6"
```
