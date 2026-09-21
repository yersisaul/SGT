/**
 * Semáforo de SLA de despacho para Solicitud/Requerimiento. Se calcula
 * siempre contra Date.now() (nunca contra un contador que solo descuenta),
 * usando fecha_registro y fecha_limite_despacho tal como las devuelve el
 * backend (SlaCalculator) — el navegador no decide el SLA, solo lee el
 * deadline ya calculado.
 *
 * Umbral (documentado, no arbitrario):
 * - VERDE: queda más del 25% de la ventana total (fecha_limite - fecha_registro).
 * - AMARILLO: queda 25% o menos, pero el deadline no se ha cumplido.
 * - ROJO: el deadline ya pasó ("Fuera de SLA").
 * Con SLA de 1h/2h/4h esto da avisos de 15min/30min/1h antes del vencimiento
 * respectivamente — proporcional a cada ventana, no un umbral fijo en minutos.
 */
export type SlaVariante = 'verde' | 'amarillo' | 'rojo';

export interface SlaEstado {
  variante: SlaVariante;
  etiqueta: string;
}

const UMBRAL_AMARILLO = 0.25;

export function calcularSla(fechaRegistro: string, fechaLimiteDespacho: string | null, ahora = Date.now()): SlaEstado | null {
  if (!fechaLimiteDespacho) return null;

  const inicio = new Date(fechaRegistro).getTime();
  const limite = new Date(fechaLimiteDespacho).getTime();
  if (Number.isNaN(inicio) || Number.isNaN(limite)) return null;

  const ventanaTotalMs = limite - inicio;
  const restanteMs = limite - ahora;

  if (restanteMs <= 0) {
    return { variante: 'rojo', etiqueta: 'Fuera de SLA' };
  }

  const proporcionRestante = ventanaTotalMs > 0 ? restanteMs / ventanaTotalMs : 0;
  const variante: SlaVariante = proporcionRestante > UMBRAL_AMARILLO ? 'verde' : 'amarillo';
  return { variante, etiqueta: `${formatearDuracion(restanteMs)} restantes` };
}

function formatearDuracion(ms: number): string {
  const totalMinutos = Math.max(0, Math.round(ms / 60000));
  const horas = Math.floor(totalMinutos / 60);
  const minutos = totalMinutos % 60;
  if (horas > 0) {
    return `${horas}h ${minutos}m`;
  }
  return `${minutos}m`;
}
