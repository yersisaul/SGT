import { BadgeVariant } from '../components/badge/badge';

/** Mapea prioridad (string libre en BD) a variante visual. Ver estado-badge.util. */
export function prioridadBadgeVariant(prioridad: string): BadgeVariant {
  const normalizado = prioridad.toLowerCase();
  if (normalizado.includes('alta')) return 'danger';
  if (normalizado.includes('media')) return 'warning';
  if (normalizado.includes('baja')) return 'neutral';
  return 'neutral';
}

export const PRIORIDAD_OPTIONS = [
  { value: 'Alta', label: 'Alta' },
  { value: 'Media', label: 'Media' },
  { value: 'Baja', label: 'Baja' },
];
