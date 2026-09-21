import { Component, computed, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';

import { calcularSla, SlaVariante } from '../../utils/sla.util';
import { Badge, BadgeVariant } from '../badge/badge';

const REFRESCO_MS = 30_000;

const VARIANTE_A_BADGE: Record<SlaVariante, BadgeVariant> = {
  verde: 'success',
  amarillo: 'warning',
  rojo: 'danger',
};

/**
 * Semáforo discreto de SLA de despacho (ver shared/utils/sla.util.ts para el
 * umbral). Se recalcula cada 30s contra Date.now() mientras esté montado, en
 * vez de mantener un contador propio que se desincroniza. No renderiza nada
 * si no hay fecha_limite_despacho (registro ya despachado o SLA no aplicable
 * — lo decide el componente contenedor mostrando/ocultando este elemento
 * según el estado actual).
 */
@Component({
  selector: 'app-sla-indicator',
  imports: [Badge],
  templateUrl: './sla-indicator.html',
})
export class SlaIndicator {
  readonly fechaRegistro = input.required<string>();
  readonly fechaLimiteDespacho = input<string | null>(null);

  private readonly tick = toSignal(interval(REFRESCO_MS), { initialValue: 0 });

  protected readonly sla = computed(() => {
    this.tick();
    return calcularSla(this.fechaRegistro(), this.fechaLimiteDespacho());
  });

  protected readonly badgeVariant = computed<BadgeVariant>(() => {
    const estado = this.sla();
    return estado ? VARIANTE_A_BADGE[estado.variante] : 'neutral';
  });
}
