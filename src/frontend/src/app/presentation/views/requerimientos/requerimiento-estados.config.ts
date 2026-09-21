/**
 * El catálogo /api/estados es compartido con Solicitud y Orden. Este
 * archivo fija, por nombre (no por posición), qué subconjunto pertenece al
 * dominio de Requerimiento — mismo criterio que
 * solicitudes/solicitud-estados.config.ts.
 *
 * Requerimiento maneja 6 estados: Pendiente, En revisión (se alcanza
 * "despachando", vía PUT), Aprobado/Rechazado (vía POST /api/aprobaciones),
 * En progreso (al generar la OT desde un Requerimiento Aprobado) y
 * Finalizado (al cerrarse la Orden asociada). Solo Pendiente/En revisión son
 * editables por el PUT genérico (RequerimientoServiceImpl.validarTransicion);
 * el resto son estados de negocio alcanzados por operaciones dedicadas.
 */
export const REQUERIMIENTO_ESTADOS_VALIDOS: readonly string[] = [
  'Pendiente',
  'En revisión',
  'Aprobado',
  'Rechazado',
  'En progreso',
  'Finalizado',
];

export const REQUERIMIENTO_ESTADOS_EDITABLES_POR_PUT: readonly string[] = ['Pendiente', 'En revisión'];

/**
 * Subconjunto mostrado como columnas del Kanban (decisión de presentación,
 * no de modelo): "Aprobado"/"Rechazado" NO tienen columna propia — son el
 * resultado de una decisión puntual, no un paso operativo del flujo. Siguen
 * existiendo como estado real y se muestran igual en la Tabla (que no usa
 * este subconjunto, ver requerimientos/components/tabla/tabla.ts).
 */
export const REQUERIMIENTO_ESTADOS_KANBAN: readonly string[] = ['Pendiente', 'En revisión', 'En progreso', 'Finalizado'];

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

export function esEstadoKanbanDeRequerimiento(nombre: string): boolean {
  const objetivo = normalizar(nombre);
  return REQUERIMIENTO_ESTADOS_KANBAN.some((estado) => normalizar(estado) === objetivo);
}
