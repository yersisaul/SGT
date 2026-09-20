/** Contrato real de EstadoController (EstadoRequest/EstadoResponse en backend).
 * El catálogo /api/estados es compartido por Solicitud, Requerimiento y Orden
 * (ver *-estados.config.ts de cada módulo): varias reglas de esos módulos
 * identifican un Estado por su "nombre" exacto (normalizado), no por id. */

export interface EstadoRequest {
  nombre: string;
}

export interface EstadoResponse {
  id_estado: string;
  nombre: string;
}

/** Nombres de Estado que el frontend usa para lógica de negocio (transiciones,
 * colores de badge) en solicitud-estados.config.ts, requerimiento-estados.config.ts
 * y orden-estados.config.ts. Renombrar o eliminar uno de estos rompe esa lógica
 * en Solicitudes/Requerimientos/Órdenes aunque el backend lo permita. */
export const ESTADOS_CRITICOS: readonly string[] = [
  'Pendiente',
  'En revisión',
  'En progreso',
  'Finalizado',
  'Aprobado',
  'Rechazado',
];

export function esEstadoCritico(nombre: string): boolean {
  const objetivo = nombre.trim().toLowerCase();
  return ESTADOS_CRITICOS.some((critico) => critico.toLowerCase() === objetivo);
}
