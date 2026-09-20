/**
 * El catálogo /api/estados es compartido con Solicitud y Orden. Este
 * archivo fija, por nombre (no por posición), qué subconjunto pertenece al
 * dominio de Requerimiento — mismo criterio que
 * solicitudes/solicitud-estados.config.ts.
 *
 * "Aprobado"/"Rechazado" son válidos para mostrar (columna del Kanban,
 * resultado de la decisión) pero NO son editables por el PUT genérico: solo
 * se alcanzan vía POST /api/aprobaciones (RequerimientoServiceImpl.validarTransicion
 * solo permite Pendiente<->En revisión).
 */
export const REQUERIMIENTO_ESTADOS_VALIDOS: readonly string[] = ['Pendiente', 'En revisión', 'Aprobado', 'Rechazado'];

export const REQUERIMIENTO_ESTADOS_EDITABLES_POR_PUT: readonly string[] = ['Pendiente', 'En revisión'];

function normalizar(nombre: string): string {
  return nombre.trim().toLowerCase();
}

export function esEstadoValidoDeRequerimiento(nombre: string): boolean {
  const objetivo = normalizar(nombre);
  return REQUERIMIENTO_ESTADOS_VALIDOS.some((valido) => normalizar(valido) === objetivo);
}

export function esEstadoEditablePorPut(nombre: string): boolean {
  const objetivo = normalizar(nombre);
  return REQUERIMIENTO_ESTADOS_EDITABLES_POR_PUT.some((editable) => normalizar(editable) === objetivo);
}

export function esEstadoAprobado(nombre: string): boolean {
  return normalizar(nombre) === 'aprobado';
}

export function esEstadoDecidido(nombre: string): boolean {
  const objetivo = normalizar(nombre);
  return objetivo === 'aprobado' || objetivo === 'rechazado';
}
