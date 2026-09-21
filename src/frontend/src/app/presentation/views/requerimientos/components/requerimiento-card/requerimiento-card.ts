import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { LucidePaperclip, LucideTrash } from '@lucide/angular';

import { Badge } from '../../../../../shared/components/badge/badge';
import { SlaIndicator } from '../../../../../shared/components/sla-indicator/sla-indicator';
import { estadoBadgeVariant } from '../../../../../shared/utils/estado-badge.util';
import { RequerimientoView } from '../../requerimiento-view.model';

@Component({
  selector: 'app-requerimiento-card',
  imports: [Badge, SlaIndicator, DatePipe, LucidePaperclip, LucideTrash],
  templateUrl: './requerimiento-card.html',
  styleUrl: './requerimiento-card.css',
})
export class RequerimientoCard {
  readonly item = input.required<RequerimientoView>();
  readonly canDelete = input(false);

  readonly open = output<void>();
  readonly delete = output<void>();

  protected readonly estadoBadgeVariant = estadoBadgeVariant;

  /** El SLA de despacho de Requerimiento mide hasta que pase a "En revisión"
   * (despacho); una vez ahí, deja de aplicar. */
  protected get mostrarSla(): boolean {
    return this.item().estadoNombre.toLowerCase() === 'pendiente';
  }

  protected onDelete(event: Event): void {
    event.stopPropagation();
    this.delete.emit();
  }
}
