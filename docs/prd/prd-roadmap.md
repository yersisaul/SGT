# Roadmap — SGT

Modo visión completa: tallas de épica **S** (1–2 semanas) · **M** (3–4 semanas) · **L** (5–8 semanas) · **XL** (9+ semanas), estimadas para **1 desarrollador full-stack**. Con 2 personas, las fases 2 y 3 se pueden solapar.

## Épicas

| Épica | Talla | Depende de | Riesgo principal |
|---|---|---|---|
| E1 Correcciones críticas y seguridad | M | — | Romper vistas del frontend que hoy dependen de ver todo |
| E7 Integridad y deuda técnica | M | — | Script SQL de arranque no idempotente (mitigado con `IF NOT EXISTS`) |
| E2 Especialidades y equipos | M | E7 (secuencias) | Falta la lista real de equipos (OQ-10) |
| E3 Cola de OT | L | E2 | Concurrencia al tomar y cambio de contrato de `reasignar` |
| E4 Ciclo de vida del RQ | M | E3 (generar en cola) | Transacción aprobar + generar OT (rollback total, AC-023) |
| E5 Notificaciones SSE | M | E3 | Proxies que cortan conexiones largas |
| E8 Frontend del flujo | L | E3, E4 (y E5 para badges) | Pantallas con demasiadas acciones; validar con usuarios |
| E6 KPIs y dashboards | L | E3 (datos de asignación), E4 | Necesita semanas de datos reales de `asignacion_orden` para ser útil |

## Fases

| Fase | Contenido | Talla acumulada | Hito de salida |
|---|---|---|---|
| F0 — Desbloqueo | E1 (hallazgos B1, S1, S2, N7 primero) | M | El arranque en BD limpia funciona y el Cliente queda aislado (AC-001 a AC-004) |
| F1 — Cimientos | E7 + E2 | 2 × M | Secuencias y script de arranque operativos, catálogo de 5 especialidades y equipos cargados |
| F2 — Núcleo del flujo | E3 + E4 + la parte de E8 de Operaciones, Despachador y modal | L + M + parte de L | Pasos 5 a 14 operables de punta a punta desde la UI |
| F3 — Tiempo real | E5 + badges de E8 | M | Avisos < 5 s (NFR-003) |
| F4 — Medición | E6 + dashboards por rol de E8 | L | KPIs con rango y CSV; líneas base de G2 a G4 publicadas |

**Duración estimada:** F0 (3–4 sem) + F1 (6–8 sem) + F2 (8–12 sem) + F3 (3–4 sem) + F4 (5–8 sem) ≈ **25–36 semanas** para 1 persona. Es una estimación de talla, no un compromiso. Se recalibra al detallar cada épica con story points.

## Diagrama de dependencias

```
E1 ─────────────────────────────┐
E7 ──► E2 ──► E3 ──┬──► E4 ──┐  │
                   ├──► E5 ──┼──► E8 ──► E6
                   └─────────┘
```
(sin ciclos)

## Riesgos

| Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|
| `ddl-auto=update` no cambia de forma fiable la nulabilidad de columnas existentes (p. ej. `orden.id_usuario` → nullable) | Alta | Medio | La BD está limpia: recrearla al desplegar E3. En entornos con datos, ejecutar el `ALTER TABLE` documentado en `db/init.sql` |
| Especialidad sin responsable activo | Media | Medio | El Admin puede reasignar (D14) y el KPI de cola lo hace visible |
| El cambio de contrato de `reasignar` rompe el frontend actual | Alta | Medio | Publicar backend y frontend de E3 en la misma versión |
| Inanición de la cola (nadie toma OT) | Media | Alto | KPI de antigüedad (FR-032) y alerta P3; escalado SG-01 si se aprueba |
| Conexiones SSE cortadas por el proxy | Media | Bajo | Heartbeat de 25 s, reconexión con backoff y refresco de respaldo cada 60 s |

## Despliegue y reversión

- Cada fase sale como una versión etiquetada.
- Reversión de aplicación: volver a desplegar la etiqueta anterior. Reversión de datos: restaurar el respaldo `pg_dump` tomado antes del despliegue (sin Flyway no hay scripts de reversión automáticos, D18).
- F2 (cambio de contrato de OT) se publica junto con su frontend. Con la BD limpia (D19) no hay OT abiertas que migrar.
