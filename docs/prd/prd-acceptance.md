# Criterios de aceptación — SGT

Formato: **DADO / CUANDO / ENTONCES**. AC-NNN valida FR-NNN. Las pruebas que validan cada criterio están en `prd-tests.md` (Parte C).

## E1 — Correcciones críticas y seguridad

| AC | FR | Criterio |
|---|---|---|
| AC-001 | FR-001 | DADO una BD PostgreSQL vacía, CUANDO arranca la aplicación, ENTONCES el seeder termina sin error y cada activo sembrado tiene una especialidad del catálogo nuevo |
| AC-002 | FR-002 | DADO un Cliente autenticado, CUANDO crea una Solicitud enviando `id_estado` = "Finalizado", ENTONCES la Solicitud se guarda en "Pendiente" |
| AC-003 | FR-003 | DADO la Solicitud ST-1 del cliente A, CUANDO el cliente B envía `PUT /solicitudes/{ST-1}`, ENTONCES recibe 403 (o 404) y ST-1 no cambia. Y DADO ST-1 en "En progreso", CUANDO A la edita, ENTONCES recibe 409 |
| AC-004 | FR-004 | DADO las OT-1 (de una Solicitud del cliente A) y OT-2 (del cliente B), CUANDO A lista `GET /ordenes`, ENTONCES solo ve OT-1, y `GET /ordenes/{OT-2}` responde 404 |
| AC-005 | FR-005 | DADO un Cliente, CUANDO consulta `GET /historial-ordenes` y `GET /usuarios`, ENTONCES solo recibe el historial de sus OT y no recibe el listado de usuarios (403 o lista recortada según la regla definida) |
| AC-006 | FR-006 | DADO el rol "Cliente" renombrado a "Clientes" con los mismos permisos, CUANDO un cliente lista OT, ENTONCES la restricción de AC-004 sigue aplicándose |
| AC-007 | FR-007 | DADO una Solicitud con OT generada, CUANDO el Despachador cambia su `id_especialidad`, ENTONCES recibe 409 y la especialidad no cambia |

## E2 — Especialidades y equipos

| AC | FR | Criterio |
|---|---|---|
| AC-008 | FR-008 | DADO el usuario U, CUANDO el Admin lo agrega a "DevOPS" y a "Desarrollo", ENTONCES U ve la cola de ambas especialidades |
| AC-009 | FR-009 | DADO "DevOPS" con los miembros U1 y U2, CUANDO el Admin marca a los dos como responsables, ENTONCES ambos pueden asignar OT de "DevOPS" y ninguno puede asignar en otra especialidad |
| AC-010 | FR-010 | DADO el sistema desplegado, CUANDO se consulta `GET /especialidades`, ENTONCES se obtienen exactamente las 5 especialidades activas del catálogo D4 |
| AC-011 | FR-011 | DADO un Admin, CUANDO quita a U de "DevOPS", ENTONCES U deja de ver esa cola y las OT que ya tenía asignadas siguen asignadas a U |
| AC-012 | FR-012 | DADO una BD vacía y la lista de equipos y activos entregada por el dueño del producto, CUANDO arranca la aplicación dos veces seguidas, ENTONCES cada miembro y responsable queda en su especialidad, cada activo en la suya, y el segundo arranque no duplica nada |

## E3 — Cola de OT

| AC | FR | Criterio |
|---|---|---|
| AC-013 | FR-013 | DADO una Solicitud "Pendiente", CUANDO el Despachador genera la OT eligiendo "DevOPS", ENTONCES la OT queda con `especialidad`=DevOPS, `usuario`=null, estado "Pendiente", y se registra una asignación ENCOLADA |
| AC-014 | FR-014 | DADO una OT en cola de "DevOPS" y dos miembros que la toman al mismo tiempo, CUANDO ambas solicitudes llegan, ENTONCES una responde 200 y la otra 409; un no miembro recibe 404 (la OT no le es visible) |
| AC-015 | FR-015 | DADO un responsable de "DevOPS" y una OT en su cola, CUANDO la asigna al miembro U, ENTONCES `usuario`=U y se registra ASIGNADA. Si U no es miembro de "DevOPS", la respuesta es 409 |
| AC-016 | FR-016 | DADO una OT "Asignada" a U, CUANDO U la verifica con `corresponde=true`, ENTONCES pasa a "En progreso" y se registra CONFIRMADA. CUANDO la verifica con `corresponde=false` y un motivo, ENTONCES queda "Devuelta", sin ejecutor, en la misma especialidad, se registra DEVUELTA y ningún miembro puede tomarla (409). Sin motivo → 400. Otro usuario → 403 |
| AC-017 | FR-017 | DADO una OT "Devuelta" de "DevOPS", CUANDO el responsable R de DevOPS (o un Admin) la reasigna a "Desarrollo" con un motivo, ENTONCES `especialidad`=Desarrollo, `usuario`=null, estado "Pendiente", y la OT aparece en la cola de "Desarrollo" y desaparece de la de "DevOPS". Sin motivo → 400. Un miembro que no es responsable → 403. Reasignar a la misma especialidad → 409 |
| AC-018 | FR-018 | DADO el escenario de AC-016 (devolución por U) y AC-017 (reasignación por R), CUANDO se consulta el historial de asignaciones, ENTONCES existen DEVUELTA (usuario origen U, actor U, motivo) y REASIGNADA_ESPECIALIDAD (origen DevOPS, destino Desarrollo, actor R, motivo), ambos con fecha |
| AC-019 | FR-019 | DADO una OT "En progreso" asignada a U, CUANDO otro usuario con `orden.cerrar` intenta cerrarla, ENTONCES recibe 403; CUANDO la cierra U, ENTONCES la OT queda "Finalizado" y la Solicitud o RQ de origen también |
| AC-020 | FR-020 | DADO U, miembro de "DevOPS", y R, responsable de "DevOPS", CUANDO cada uno abre sus vistas, ENTONCES U ve la cola de DevOPS y sus OT, y R además ve las OT de cada miembro con su conteo de abiertas |

## E4 — Ciclo de vida del RQ

| AC | FR | Criterio |
|---|---|---|
| AC-021 | FR-021 | DADO el RQ-1 originado en ST-1 ("En revisión"), CUANDO el Admin lo rechaza con el motivo "Fuera de alcance", ENTONCES RQ-1 queda "Rechazado", ST-1 queda "Rechazado" y el historial de ST-1 contiene el motivo |
| AC-022 | FR-022 | DADO el escenario de AC-021, CUANDO el cliente dueño de ST-1 abre su seguimiento, ENTONCES ve el estado "Rechazado" y el texto "Fuera de alcance" |
| AC-023 | FR-023 | DADO el RQ-2 "En revisión", CUANDO el Admin confirma el modal con la especialidad "Implementación", ENTONCES en una sola transacción RQ-2 queda "En progreso", se crea la OT en la cola de "Implementación" y la Solicitud de origen pasa a "En progreso". Si falla la creación de la OT, nada se persiste |
| AC-024 | FR-024 | DADO el RQ-3 "En revisión", CUANDO se registra la aprobación sin especialidad de OT, ENTONCES RQ-3 queda "Aprobado", su Solicitud sigue "En revisión" y `POST /requerimientos/{RQ-3}/generar-orden` funciona después |
| AC-025 | FR-025 | DADO una aprobación con `aprobado` nulo, CUANDO se envía, ENTONCES la respuesta es 400 con el detalle del campo y no se crea ningún registro |

## E5 — Notificaciones

| AC | FR | Criterio |
|---|---|---|
| AC-026 | FR-026 | DADO U conectado al stream y miembro de "DevOPS", CUANDO se encola o se reasigna una OT a "DevOPS", ENTONCES U recibe el evento `orden.encolada` en menos de 5 s; un usuario de otra especialidad no lo recibe |
| AC-027 | FR-027 | DADO U conectado, CUANDO un responsable le asigna una OT, ENTONCES U recibe `orden.asignada` con `numero_orden` y sin datos personales |
| AC-028 | FR-028 | DADO el badge de cola en 3, CUANDO llega `orden.encolada`, ENTONCES muestra 4 sin recargar la página |
| AC-029 | FR-029 | DADO un corte de red de 20 s, CUANDO vuelve la conexión, ENTONCES el cliente reconecta con backoff y los contadores coinciden con `GET /ordenes/cola` |

## E6 — KPIs y dashboards

| AC | FR | Criterio |
|---|---|---|
| AC-030 | FR-030 | DADO 10 Solicitudes de prioridad "Alta" en el rango, 9 despachadas dentro de su SLA, CUANDO se consulta `sla-despacho`, ENTONCES devuelve 90 % para "Alta" |
| AC-031 | FR-031 | DADO un conjunto sintético con tiempos de cola conocidos [1 h, 2 h, 3 h], CUANDO se consulta `tiempos-ciclo`, ENTONCES la mediana de cola→toma es 2 h |
| AC-032 | FR-032 | DADO 4 OT en cola de "DevOPS" (la más antigua de hace 6 h), CUANDO se consulta `cola-carga`, ENTONCES devuelve cola=4 y antigüedad máxima=6 h para DevOPS |
| AC-033 | FR-033 | DADO 20 OT generadas, de las cuales 3 se reasignaron de especialidad, CUANDO se consulta `ruteo`, ENTONCES la tasa de reasignación es 15 % |
| AC-034 | FR-034 | DADO usuarios de cada rol, CUANDO abren `/app/dashboard`, ENTONCES cada uno ve los widgets de su rol (según la tabla de FR-034) y ningún widget fuera de sus permisos |
| AC-035 | FR-035 | DADO el rango "semana", CUANDO se cambia a "personalizado" entre dos fechas, ENTONCES todos los widgets se recalculan para ese rango; un rango con `desde > hasta` o de más de 366 días responde 400 |
| AC-036 | FR-036 | DADO un responsable con rango de mes, CUANDO exporta `cola-carga` a CSV, ENTONCES el archivo contiene las mismas cifras que la pantalla, está en UTF-8 y las celdas que empiezan con `=` quedan neutralizadas |
| AC-037 | FR-037 | DADO un responsable de "DevOPS" que pide `cola-carga` con `id_especialidad`=Desarrollo, CUANDO envía la petición, ENTONCES recibe 403 (o datos vacíos); un Cliente que llama a `/kpis/*` recibe 403 |

## E7 — Integridad

| AC | FR | Criterio |
|---|---|---|
| AC-038 | FR-038 | DADO 50 Solicitudes creadas en paralelo y luego el borrado de ST-10, CUANDO se crea otra, ENTONCES los 51 números son únicos y ninguno se repite |
| AC-039 | FR-039 | DADO una Solicitud con historial, CUANDO un Admin envía `DELETE`, ENTONCES recibe 409 con `code` y el registro permanece |
| AC-040 | FR-040 | DADO una BD vacía, CUANDO arranca la aplicación, ENTONCES existen las secuencias `seq_solicitud`, `seq_requerimiento`, `seq_orden` y el índice parcial de cola; en un segundo arranque el script no falla |
| AC-041 | FR-041 | DADO un `id_especialidad` inexistente al crear una OT o un activo, CUANDO se envía, ENTONCES la respuesta es 404 (no 500) |
| AC-042 | FR-042 | DADO un body de generar OT con `url_adjunto`, CUANDO se envía, ENTONCES el campo se ignora o se rechaza con 400, y el adjunto solo se puede subir por `/api/archivos/ordenes/{id}` |

## E8 — Frontend

| AC | FR | Criterio |
|---|---|---|
| AC-043 | FR-043 | DADO 3 Solicitudes pendientes con vencimientos distintos, CUANDO el Despachador abre su bandeja, ENTONCES aparecen ordenadas por vencimiento; al despachar solo se elige la especialidad (no hay selector de persona) |
| AC-044 | FR-044 | DADO un miembro de Operaciones, CUANDO abre Órdenes, ENTONCES tiene las pestañas "Cola" y "Mis OT" con las acciones Tomar, Confirmar, No me corresponde y Cerrar, habilitadas según el estado |
| AC-045 | FR-045 | DADO un responsable, CUANDO abre la vista de Equipo, ENTONCES ve la carga por miembro y puede asignar una OT de la cola a un miembro de la lista |
| AC-046 | FR-046 | DADO un Admin en un RQ "En revisión", CUANDO pulsa Aprobar, ENTONCES se abre un modal con las 5 especialidades (la del RQ preseleccionada); al confirmar se aprueba y se crea la OT; al cancelar, la aprobación se registra igual (RQ "Aprobado", Solicitud "En revisión") y queda disponible el botón "Generar OT" |
| AC-047 | FR-047 | DADO un Cliente con ST-1 rechazada, CUANDO abre el detalle, ENTONCES el timeline muestra cada cambio de estado con su fecha y el motivo de rechazo |
| AC-048 | FR-048 | DADO un Admin en Especialidades > Equipo, CUANDO agrega a U como responsable, ENTONCES el cambio se persiste y se refleja en AC-009 |

> AC-046 actualizado con la decisión D17 (2026-10-03).
