/**
 * El catálogo /api/estados es compartido entre Solicitud, Requerimiento y
 * Orden (mismo Estado.nombre para las tres). Esta configuración fija, por
 * nombre (no por posición), qué subconjunto es válido para Solicitud y
 * cuáles de esos son alcanzables por el PUT genérico.
 *
 * Solicitud maneja ÚNICAMENTE Pendiente/En progreso/Finalizado — "En
 * revisión" no es un estado propio de Solicitud (se eliminó del flujo:
 * "despachar" una Solicitud significa generar la OT directamente, sin un
 * paso manual intermedio). Ninguno de los tres es editable por PUT genérico:
 * "Pendiente -> En progreso" solo se alcanza vía POST
 * /solicitudes/{id}/generar-orden, y "-> Finalizado" solo cuando se cierra la
 * Orden asociada (SolicitudServiceImpl.validarTransicion no permite ninguna
 * transición manual). "Aprobado"/"Rechazado"/"En revisión" son de
 * Requerimiento y nunca deben aparecer acá.
 */
export const SOLICITUD_ESTADOS_VALIDOS: readonly string[] = ['Pendiente', 'En progreso', 'Finalizado'];

export const SOLICITUD_ESTADOS_EDITABLES_POR_PUT: readonly string[] = [];

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

// NO usar "no editable por PUT" como proxy de "finalizada": desde que
// SOLICITUD_ESTADOS_EDITABLES_POR_PUT quedó vacío (ninguna transición de
// Solicitud es manual), esa comparación daba "finalizada" para CUALQUIER
// estado, incluido Pendiente. Comparar directamente por nombre de estado.
export function esEstadoPendiente(nombre: string): boolean {
  return normalizar(nombre) === 'pendiente';
}

export function esEstadoFinalizado(nombre: string): boolean {
  return normalizar(nombre) === 'finalizado';
}
