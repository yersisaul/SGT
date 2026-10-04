/**
 * Estados de Orden del catálogo compartido /api/estados (PRD D13):
 * Pendiente (en cola) → Asignada → En progreso → Finalizado, y Devuelta
 * cuando el ejecutor declara que no le corresponde. Ningún estado se cambia
 * por arrastre ni por PUT: la OT avanza solo con operaciones de negocio.
 */
export const ORDEN_ESTADOS_VALIDOS: readonly string[] = ['Pendiente', 'Devuelta', 'Asignada', 'En progreso', 'Finalizado'];

function normalizar(nombre: string): string {
  return nombre.trim().toLowerCase();
}

export function esEstadoValidoDeOrden(nombre: string): boolean {
  const objetivo = normalizar(nombre);
  return ORDEN_ESTADOS_VALIDOS.some((valido) => normalizar(valido) === objetivo);
}

export function esEstadoFinalizado(nombre: string): boolean {
  return normalizar(nombre) === 'finalizado';
}

export function esEstadoPendiente(nombre: string): boolean {
  return normalizar(nombre) === 'pendiente';
}

export function esEstadoAsignada(nombre: string): boolean {
  return normalizar(nombre) === 'asignada';
}

export function esEstadoDevuelta(nombre: string): boolean {
  return normalizar(nombre) === 'devuelta';
}

export function esEstadoEnProgreso(nombre: string): boolean {
  return normalizar(nombre) === 'en progreso';
}
