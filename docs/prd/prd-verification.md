# Informe de verificación — PRD SGT

> **Nota de transparencia:** este PRD se generó en modo CLI **sin** el binario del motor de verificación del plugin. No hubo jueces múltiples ni consenso automático, y no hay telemetría de ejecución. Las comprobaciones de la sección 1 se ejecutaron con un script (`grep`/`awk`) sobre los archivos y cualquiera puede repetirlas. Los veredictos de la sección 2 son una autoevaluación del modelo que generó el documento.

## 1. Integridad estructural (determinista)

| Comprobación | Resultado | Evidencia |
|---|---|---|
| IDs de FR únicos y sin huecos | PASS | 48 definidos, 48 únicos (FR-001…FR-048) |
| IDs de NFR únicos y sin huecos | PASS | 11 (NFR-001…NFR-011) |
| Cada FR tiene fuente (trazabilidad) | PASS | Columna "Fuente" en todas las filas; lo no solicitado se separó como `[SUGGESTED]` SG-01…SG-04 |
| Cada FR tiene al menos un AC | PASS | 48/48 (AC-NNN ↔ FR-NNN) |
| Cada AC tiene prueba | PASS | 48/48 (T-NNN ↔ AC-NNN, paridad verificada por script: 0 discrepancias) |
| AC referenciados en historias | PASS | 48/48 |
| AC de JIRA = AC del PRD (sin numeración propia) | PASS | 48/48 IDs idénticos |
| Paridad JIRA ↔ historias | PASS | SGT-N ↔ US-N, 0 discrepancias |
| Sin auto-dependencias | PASS | 0 detectadas en JIRA ni en historias |
| Grafo de dependencias acíclico | PASS | E7→E2→E3→{E4,E5}→E8→E6; E1 independiente |
| Sin columna de SP en la tabla de FR | PASS | 0 apariciones |
| Recuento de tallas | PASS | 8 S + 15 M + 5 L = 28 historias, igual en historias y JIRA |
| Pruebas sin cuerpo (placeholders) | PASS | No hay funciones de prueba; todas son especificaciones y se declaran como tales ("0 implementadas · 48 especificadas") |
| DDL huérfano / `NOW()` en índices parciales | N/A | El modo visión completa no incluye DDL; el índice parcial propuesto usa `usuario IS NULL` (estable) |
| Aritmética de SP / distribución desigual | N/A | Sin story points en este modo |

## 2. Registro de verificación de afirmaciones

| Afirmación | Veredicto | Comentario |
|---|---|---|
| Las brechas N1, N2, N4, S1, S2 y B1 existen en el código | PASS | Verificadas leyendo el código fuente; rutas en `docs/AUDITORIA.md` |
| El modelo N:M con `es_responsable` cubre D2, D3 y D5 | PASS | Trazado en FR-008, FR-009, FR-015 |
| La toma atómica con `UPDATE … WHERE usuario IS NULL` evita la doble toma | PASS (de diseño) | Correcto bajo READ COMMITTED en PostgreSQL; se confirma en T-014 |
| SSE sin token en la URL usando `fetch` + `ReadableStream` | PASS (de diseño) | Evita la limitación de `EventSource`; no requiere dependencias |
| Aviso SSE < 5 s p95 (NFR-003) | SPEC-COMPLETE | Prueba k6 especificada; sin datos de ejecución |
| KPIs < 1,5 s p95 con 50 000 OT (NFR-004) | SPEC-COMPLETE | Depende de índices y del volumen real |
| Heartbeat de 25 s suficiente frente al proxy | NEEDS-RUNTIME | Depende del proxy de producción (no se conoce) |
| Duración de 25–36 semanas | INCONCLUSIVE | Estimación por tallas; depende del equipo real |
| Valores objetivo de G2–G4 (90 %, < 2 h, < 15 %) | INCONCLUSIVE | Propuestos sin línea base (OQ-07) |
| Los permisos nuevos se pueden usar en F2 | PASS | Autorizados el 2026-10-03 (D12) |
| Equipos sembrados reflejan la organización real | INCONCLUSIVE | Depende de la lista del dueño del producto (OQ-10) |
| Cobertura 80 % / 70 % alcanzable | SPEC-COMPLETE | JaCoCo en CI especificado |

Distribución (actualizada 2026-10-03): 7 PASS · 3 SPEC-COMPLETE · 1 NEEDS-RUNTIME · 3 INCONCLUSIVE · 0 FAIL (de 14 afirmaciones de contenido; las comprobaciones estructurales de la sección 1 van aparte).

## 3. Correcciones durante el autochequeo

| Antes | Después |
|---|---|
| Ninguna violación estructural detectada por el script | — |

## 4. Señales de auditoría (advertencias de calidad)

| Regla | Hallazgo | Acción sugerida |
|---|---|---|
| CITE | Los umbrales de 5 s, 1,5 s, 25 s y 366 días no tienen fuente externa; son decisiones de diseño | Validarlos en la revisión técnica |
| CITE | Los objetivos de G2–G4 son propuestas sin línea base | Calcular la línea base en E6 y reajustar (OQ-07) |
| TECH | Micrometer/Actuator sería una dependencia nueva (Flyway descartado, D18) | Aprobarla explícitamente antes de E6 |
| TECH | La librería de gráficos del frontend no está elegida | Decidir en E6/E8 con criterio de licencia y peso |
| UX | ~~Ambigüedad en "cancelar el modal"~~ resuelta por D17 | — |
| OPS | Registro SSE y rate limit en memoria → una sola instancia | Documentar como restricción de despliegue |
| SEC | El JWT sigue en `localStorage` | Evaluar SG-02 |

Tasa: 7 señales sobre 14 afirmaciones (50 %). Es alta porque el modo visión completa deja deliberadamente decisiones abiertas para la revisión.

## 5. Métricas operativas

No se registraron (sin motor de verificación). No se reportan tokens, número de llamadas ni costos para no presentar cifras sin medición.

## 6. Limitaciones y revisión humana requerida

1. ~~OQ-01, OQ-02, OQ-03, OQ-06 y cambios de BD~~: cerrados el 2026-10-03 (D12–D19).
2. Recibir la lista de equipos y activos (OQ-10) para completar el seed de E2.
3. Validar los objetivos de G2 a G4 con el negocio.
4. Al pasar una épica a ejecución, generar su PRD enfocado (DDL, story points y sprints).

## 6b. Estado de implementación (2026-10-03)

| Etapa | Alcance | Evidencia |
|---|---|---|
| 1 | E1 + E7 (seguridad por recurso, integridad) | Suite backend |
| 2 | E2 (equipos N:M con responsable) | `FlujoNegocioIntegrationTest` |
| 3 | E3 + E4 (cola, verificación, devolución, reasignación, aprobación con OT, rechazo) | `FlujoNegocioIntegrationTest` (11), incluida concurrencia |
| 4 | E5 (SSE) | `NotificacionSseIntegrationTest` |
| 5 | E8 (frontend del flujo) | `ng build` OK; **sin e2e ni verificación visual** |
| 6 | E6 (KPIs y dashboard) | `KpiIntegrationTest`, `EstadisticaYConteoTest`, `CsvExporterTest` |

Total backend: **52 pruebas, 0 fallas**. Detalle por FR en `prd-requirements.md` §4.

## 7. Valor entregado

| Entregable | Estado | Valor |
|---|---|---|
| 8 épicas con talla, dependencias y riesgos | Listo para revisión | Base de planificación y presupuesto |
| 48 FR y 11 NFR trazados a fuentes reales | Listo | Evita requisitos inventados |
| 48 AC en GIVEN/WHEN/THEN, 1:1 con las pruebas | Listo | QA puede preparar casos desde ya |
| Diseño técnico (cola, concurrencia, SSE, KPIs) | Borrador | Reduce el riesgo de E3 y E5 |
| CSV de JIRA para importar | Listo | Carga directa del backlog |

**Listo para:** revisión con stakeholders ✅ · decisión de OQ ✅ · PRD enfocado de E1 o E3 ✅ · importación a JIRA ✅.

**Próximos pasos:** (1) revisar y cerrar OQ-01, 02, 03 y 06 → (2) aprobar los cambios de BD → (3) ejecutar F0 (E1), que no depende de ninguna decisión pendiente → (4) generar el PRD enfocado de E3.
