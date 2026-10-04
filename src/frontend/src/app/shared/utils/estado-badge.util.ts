import { BadgeVariant } from '../components/badge/badge';

/**
 * Mapea el nombre de un Estado (texto libre en BD) a una variante visual.
 * Por nombre parcial en vez de igualdad exacta: tolera acentos/variantes
 * futuras del catálogo de Estado sin romper el color-coding.
 */
export function estadoBadgeVariant(nombre: string): BadgeVariant {
  const normalizado = nombre.toLowerCase();
  if (normalizado.includes('cancel') || normalizado.includes('rechaz')) return 'danger';
  // OT devuelta por el ejecutor: requiere decisión del responsable.
  if (normalizado.includes('devuelt')) return 'warning';
  if (normalizado.includes('final') || normalizado.includes('aprob')) return 'success';
  if (normalizado.includes('progreso') || normalizado.includes('revisi') || normalizado.includes('asignad')) return 'info';
  return 'neutral';
}
