/**
 * El catálogo /api/estados es compartido entre Solicitud, Requerimiento y
 * Orden (mismo Estado.nombre para las tres). Esta configuración fija, por
 * nombre (no por posición), qué subconjunto es válido para Solicitud y
 * cuáles de esos son alcanzables por el PUT genérico.
 *
 * Solicitud maneja Pendiente/En revisión/En progreso/Finalizado. "En
 * revisión" se alcanza cuando el Despachador determina que la Solicitud está
 * fuera de contrato y genera un Requerimiento desde ella (POST
 * /solicitudes/{id}/generar-requerimiento) — la Solicitud queda vinculada al
 * flujo de aprobación del Requerimiento. Ninguno de los cuatro es editable
 * por PUT genérico: "Pendiente -> En progreso" solo se alcanza vía POST
 * /solicitudes/{id}/generar-orden (bajo contrato), "Pendiente -> En revisión"
 * solo vía generar-requerimiento (fuera de contrato), y "-> Finalizado" solo
 * cuando se cierra la Orden asociada, directamente o a través del
 * Requerimiento que generó (SolicitudServiceImpl.validarTransicion no
 * permite ninguna transición manual). "Aprobado"/"Rechazado" son exclusivos
 * de Requerimiento y nunca deben aparecer acá.
 */
export const SOLICITUD_ESTADOS_VALIDOS: readonly string[] = ['Pendiente', 'En revisión', 'En progreso', 'Finalizado', 'Rechazado'];

export const SOLICITUD_ESTADOS_EDITABLES_POR_PUT: readonly string[] = [];

/**
 * Subconjunto mostrado como columnas del Kanban (decisión de presentación):
 * "En revisión" NO tiene columna propia — mientras está ahí, la Solicitud
 * está siendo tratada por el flujo de aprobación del Requerimiento, no por
 * el flujo operativo directo de la Solicitud. Sigue existiendo como estado
 * real y se muestra igual en la Tabla (que no usa este subconjunto).
 */
export const SOLICITUD_ESTADOS_KANBAN: readonly string[] = ['Pendiente', 'En progreso', 'Finalizado', 'Rechazado'];

function normalizar(nombre: string): string {
  return nombre.trim().toLowerCase();
}

export function esEstadoValidoDeSolicitud(nombre: string): boolean {
  const objetivo = normalizar(nombre);
  return SOLICITUD_ESTADOS_VALIDOS.some((valido) => normalizar(valido) === objetivo);
}

export function esEstadoEditablePorPut(nombre: string): boolean {
  const objetivo = normalizar(nombre);
  return SOLICITUD_ESTADOS_EDITABLES_POR_PUT.some((editable) => normalizar(editable) === objetivo);
}

export function esEstadoKanbanDeSolicitud(nombre: string): boolean {
  const objetivo = normalizar(nombre);
  return SOLICITUD_ESTADOS_KANBAN.some((estado) => normalizar(estado) === objetivo);
}

// NO usar "no editable por PUT" como proxy de "finalizada": desde que
// SOLICITUD_ESTADOS_EDITABLES_POR_PUT quedó vacío (ninguna transición de
// Solicitud es manual), esa comparación daba "finalizada" para CUALQUIER
// estado, incluido Pendiente. Comparar directamente por nombre de estado.
export function esEstadoPendiente(nombre: string): boolean {
  return normalizar(nombre) === 'pendiente';
}

export function esEstadoEnRevision(nombre: string): boolean {
  return normalizar(nombre) === 'en revisión';
}

export function esEstadoFinalizado(nombre: string): boolean {
  return normalizar(nombre) === 'finalizado';
}

/** RQ rechazado por el Administrador → la Solicitud queda "Rechazado" (paso 8, PRD D6). */
export function esEstadoRechazado(nombre: string): boolean {
  return normalizar(nombre) === 'rechazado';
}
