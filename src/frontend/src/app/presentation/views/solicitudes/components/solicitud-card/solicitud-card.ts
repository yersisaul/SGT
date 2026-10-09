import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { LucidePaperclip, LucideTrash } from '../../../../../shared/icons/lucide-icons';

import { Badge } from '../../../../../shared/components/badge/badge';
import { SlaIndicator } from '../../../../../shared/components/sla-indicator/sla-indicator';
import { estadoBadgeVariant } from '../../../../../shared/utils/estado-badge.util';
import { prioridadBadgeVariant } from '../../../../../shared/utils/prioridad-badge.util';
import { SolicitudView } from '../../solicitud-view.model';

@Component({
  selector: 'app-solicitud-card',
  imports: [Badge, SlaIndicator, DatePipe, LucidePaperclip, LucideTrash],
  templateUrl: './solicitud-card.html',
  styleUrl: './solicitud-card.css',
})
export class SolicitudCard {
  readonly item = input.required<SolicitudView>();
  readonly canDelete = input(false);

  readonly open = output<void>();
  readonly delete = output<void>();

  protected readonly estadoBadgeVariant = estadoBadgeVariant;
  protected readonly prioridadBadgeVariant = prioridadBadgeVariant;

  /** El SLA de despacho solo aplica mientras la Solicitud sigue "Pendiente"
   * Se decide por el nombre de estado real, no por un flag propio. */
  protected get mostrarSla(): boolean {
    return this.item().estadoNombre.toLowerCase() === 'pendiente';
  }

  protected onDelete(event: Event): void {
    event.stopPropagation();
    this.delete.emit();
  }
}
