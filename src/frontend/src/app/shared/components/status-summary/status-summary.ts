import { Component, input, output } from '@angular/core';

import { EstadoCantidad } from '../../../core/models/dashboard.model';
import { estadoBadgeVariant } from '../../utils/estado-badge.util';
import { Badge } from '../badge/badge';
import { Button } from '../button/button';
import { Card } from '../card/card';
import { Spinner } from '../spinner/spinner';

/**
 * Panel de resumen por estado, reutilizable entre Solicitudes y
 * Requerimientos (misma forma de datos: total + conteo por estado).
 */
@Component({
  selector: 'app-status-summary',
  imports: [Card, Badge, Spinner, Button],
  templateUrl: './status-summary.html',
  styleUrl: './status-summary.css',
})
export class StatusSummary {
  readonly title = input.required<string>();
  readonly total = input(0);
  readonly items = input<EstadoCantidad[]>([]);
  readonly loading = input(false);
  readonly errorMessage = input<string | null>(null);

  readonly retry = output<void>();

  protected readonly badgeVariant = estadoBadgeVariant;

  protected percent(cantidad: number): number {
    const totalValue = this.total();
    return totalValue > 0 ? Math.round((cantidad / totalValue) * 100) : 0;
  }
}
