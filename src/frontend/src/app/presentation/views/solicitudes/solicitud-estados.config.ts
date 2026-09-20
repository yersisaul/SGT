/**
 * El catálogo /api/estados es compartido entre Solicitud, Requerimiento y
 * Orden (mismo Estado.nombre para las tres). Esta configuración fija, por
 * nombre (no por posición), qué subconjunto es válido para Solicitud y
 * cuáles de esos son alcanzables por el PUT genérico.
 *
 * "Finalizado" es válido para mostrar (columna del Kanban, aparece una vez
 * que ya se generó la Orden) pero NO es editable por PUT: solo se alcanza
 * vía POST /solicitudes/{id}/generar-orden (SolicitudServiceImpl,
 * validarTransicion). "Aprobado"/"Rechazado" son exclusivos de Requerimiento
 * y nunca deben aparecer acá.
 *
 * Cuando construyamos Requerimientos/Órdenes, cada uno tendrá su propio
 * archivo de configuración análogo a este (sus subconjuntos son distintos:
 * Requerimiento no incluye "En progreso"/"Finalizado", Orden no incluye
 * "Aprobado"/"Rechazado").
 */
export const SOLICITUD_ESTADOS_VALIDOS: readonly string[] = ['Pendiente', 'En revisión', 'En progreso', 'Finalizado'];

export const SOLICITUD_ESTADOS_EDITABLES_POR_PUT: readonly string[] = ['Pendiente', 'En revisión', 'En progreso'];

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
