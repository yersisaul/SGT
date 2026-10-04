# Historias de usuario — SGT

Modo visión completa: talla por historia (S, M o L), sin story points. El detalle GIVEN/WHEN/THEN de cada `AC-XXX` está en `prd-acceptance.md`; aquí AC-NNN valida FR-NNN (correspondencia 1:1).

| Historia | Épica | Como… | Quiero… | Para… | FR | AC | Talla | Depende de |
|---|---|---|---|---|---|---|---|---|
| US-01 | E1 | Administrador de la plataforma | que el sistema arranque en una BD nueva con el catálogo vigente | poder desplegar ambientes limpios | FR-001 | AC-001 | S | — |
| US-02 | E1 | Cliente | que mi solicitud siempre entre como "Pendiente" | que el Despachador la revise (paso 3) | FR-002 | AC-002 | S | — |
| US-03 | E1 | Cliente | editar solo mis solicitudes pendientes y ver solo mis OT | proteger mi información y la de otros clientes | FR-003, FR-004 | AC-003, AC-004 | M | — |
| US-04 | E1 | Responsable de seguridad | que la visibilidad se decida por permisos y recurso en un solo componente | evitar fugas al renombrar roles | FR-005, FR-006 | AC-005, AC-006 | M | US-03 |
| US-05 | E1 | Despachador | que una Solicitud despachada no cambie de especialidad ni de activo | mantener coherente la OT o el RQ generado | FR-007 | AC-007 | S | — |
| US-06 | E2 | Administrador | asignar personas a una o varias especialidades y marcar responsables | que el sistema sepa quién atiende qué | FR-008, FR-009, FR-011 | AC-008, AC-009, AC-011 | M | — |
| US-07 | E2 | Administrador | contar con el catálogo de 5 especialidades y migrar las OT existentes | reflejar la organización real de Soporte | FR-010, FR-012 | AC-010, AC-012 | M | US-06 |
| US-08 | E3 | Despachador o Admin | que la OT generada vaya a la cola de la especialidad sin elegir persona | despachar más rápido (pasos 5 y 9) | FR-013 | AC-013 | M | US-06 |
| US-09 | E3 | Miembro de Operaciones | tomar una OT de la cola de mis especialidades | empezar a trabajar de forma proactiva | FR-014 | AC-014 | M | US-08 |
| US-10 | E3 | Responsable de especialidad | asignar una OT de la cola a un miembro de mi equipo | balancear la carga | FR-015 | AC-015 | M | US-08 |
| US-11 | E3 | Ejecutor | confirmar que la OT me corresponde o declarar que no | cumplir el paso 11 con trazabilidad | FR-016 | AC-016 | S | US-09 |
| US-12 | E3 | Ejecutor | reasignar la OT a la especialidad correcta con un motivo | que llegue a quien debe atenderla (paso 12) | FR-017, FR-018 | AC-017, AC-018 | L | US-08 |
| US-13 | E3 | Ejecutor | cerrar mi OT y que se finalice el origen | completar el ciclo (paso 14) | FR-019 | AC-019 | S | US-09 |
| US-14 | E3 | Miembro o responsable | ver la cola, mis OT y la carga del equipo | organizar el trabajo diario | FR-020 | AC-020 | M | US-09, US-10 |
| US-15 | E4 | Cliente | enterarme de que mi solicitud fue rechazada y por qué | no quedar esperando indefinidamente (paso 8) | FR-021, FR-022 | AC-021, AC-022 | M | — |
| US-16 | E4 | Administrador | aprobar un RQ y generar su OT en un solo paso, eligiendo la especialidad | evitar RQ aprobados sin OT (paso 9) | FR-023, FR-024, FR-025 | AC-023, AC-024, AC-025 | M | US-08 |
| US-17 | E5 | Miembro de Operaciones | recibir un aviso inmediato cuando entra una OT a mi cola o me asignan una | atenderla sin refrescar la pantalla | FR-026, FR-027, FR-028, FR-029 | AC-026, AC-027, AC-028, AC-029 | L | US-08, US-10 |
| US-18 | E6 | Admin o Despachador | medir el SLA de despacho y los tiempos de ciclo | detectar cuellos de botella | FR-030, FR-031 | AC-030, AC-031 | L | US-12 |
| US-19 | E6 | Responsable o Admin | ver la cola, la carga y la calidad del ruteo | redistribuir el trabajo y mejorar la clasificación | FR-032, FR-033 | AC-032, AC-033 | M | US-12, US-15 |
| US-20 | E6 | Cada rol | ver un dashboard propio con rango de fechas y exportar a CSV | tomar decisiones con datos de mi ámbito | FR-034, FR-035, FR-036, FR-037 | AC-034, AC-035, AC-036, AC-037 | L | US-18, US-19 |
| US-21 | E7 | Equipo técnico | numeración única, borrados seguros y migraciones versionadas | integridad y auditabilidad | FR-038, FR-039, FR-040 | AC-038, AC-039, AC-040 | M | — |
| US-22 | E7 | Consumidor de la API | errores 404 coherentes y adjuntos solo por el fileserver | contratos predecibles | FR-041, FR-042 | AC-041, AC-042 | S | — |
| US-23 | E8 | Despachador | una bandeja ordenada por SLA donde despacho eligiendo la especialidad | cumplir el SLA (pasos 3 a 6) | FR-043 | AC-043 | M | US-08 |
| US-24 | E8 | Miembro u Operaciones | una vista de cola y de mis OT con todas las acciones del flujo | operar los pasos 10 a 14 desde una sola pantalla | FR-044 | AC-044 | L | US-09, US-11, US-12 |
| US-25 | E8 | Responsable | una vista de equipo con carga y la acción Asignar | balancear visualmente | FR-045 | AC-045 | M | US-10 |
| US-26 | E8 | Administrador | el modal de aprobación con la especialidad | completar D7 sin fricción | FR-046 | AC-046 | S | US-16 |
| US-27 | E8 | Cliente | un timeline de seguimiento de mi solicitud | saber en qué estado está (paso 2) | FR-047 | AC-047 | M | US-15 |
| US-28 | E8 | Administrador | una pantalla para gestionar miembros y responsables | mantener los equipos actualizados | FR-048 | AC-048 | S | US-06 |

## Resumen por épica

| Épica | Historias | Tallas (S/M/L) |
|---|---|---|
| E1 | US-01 a US-05 (5) | 3 S · 2 M · 0 L |
| E2 | US-06, US-07 (2) | 0 S · 2 M · 0 L |
| E3 | US-08 a US-14 (7) | 2 S · 4 M · 1 L |
| E4 | US-15, US-16 (2) | 0 S · 2 M · 0 L |
| E5 | US-17 (1) | 0 S · 0 M · 1 L |
| E6 | US-18 a US-20 (3) | 0 S · 1 M · 2 L |
| E7 | US-21, US-22 (2) | 1 S · 1 M · 0 L |
| E8 | US-23 a US-28 (6) | 2 S · 3 M · 1 L |
| **Total** | **28** | **8 S · 15 M · 5 L** |
