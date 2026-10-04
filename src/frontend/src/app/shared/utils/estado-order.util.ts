/**
 * Orden visual de columnas Kanban por nombre de Estado (catálogo real de
 * backend, no hardcodeado por id). Mismo criterio tolerante que
 * estado-badge.util: por substring, no igualdad exacta. Un Estado que no
 * calce con ningún patrón conocido (ej. "Cancelado") se ubica al final en
 * vez de perderse.
 *
 * "Aprobado"/"Rechazado" (exclusivos de Requerimiento) van entre "En
 * revisión" y "En progreso": son el resultado de la decisión que sigue a la
 * revisión y preceden a la ejecución.
 */
export function estadoOrderRank(nombre: string): number {
  const normalizado = nombre.toLowerCase();
  if (normalizado.includes('pendiente')) return 0;
  if (normalizado.includes('revisi') || normalizado.includes('devuelt')) return 1;
  if (normalizado.includes('aprob') || normalizado.includes('asignad')) return 2;
  if (normalizado.includes('progreso')) return 3;
  if (normalizado.includes('final')) return 4;
  // Terminal negativo: al final, después de "Finalizado".
  if (normalizado.includes('rechaz')) return 5;
  return 99;
}
