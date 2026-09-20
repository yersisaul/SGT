/**
 * El catálogo /api/estados es compartido con Solicitud y Requerimiento.
 * Este archivo fija, por nombre (no por posición), el subconjunto de Orden
 * — mismo criterio que solicitud-estados.config.ts / requerimiento-estados.config.ts.
 *
 * "Finalizado" es válido para mostrar (columna del Kanban) pero NO editable
 * por PUT: solo se alcanza vía POST /api/ordenes/{id}/cerrar
 * (OrdenServiceImpl.validarTransicion solo permite moverse entre
 * Pendiente/En revisión/En progreso).
 */
export const ORDEN_ESTADOS_VALIDOS: readonly string[] = ['Pendiente', 'En revisión', 'En progreso', 'Finalizado'];

export const ORDEN_ESTADOS_EDITABLES_POR_PUT: readonly string[] = ['Pendiente', 'En revisión', 'En progreso'];

function normalizar(nombre: string): string {
  return nombre.trim().toLowerCase();
}

export function esEstadoValidoDeOrden(nombre: string): boolean {
  const objetivo = normalizar(nombre);
  return ORDEN_ESTADOS_VALIDOS.some((valido) => normalizar(valido) === objetivo);
}

export function esEstadoEditablePorPut(nombre: string): boolean {
  const objetivo = normalizar(nombre);
  return ORDEN_ESTADOS_EDITABLES_POR_PUT.some((editable) => normalizar(editable) === objetivo);
}

export function esEstadoFinalizado(nombre: string): boolean {
  return normalizar(nombre) === 'finalizado';
}
