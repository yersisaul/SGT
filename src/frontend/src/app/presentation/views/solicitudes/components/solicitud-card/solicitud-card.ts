import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { LucidePaperclip, LucideTrash } from '@lucide/angular';

import { Badge } from '../../../../../shared/components/badge/badge';
import { estadoBadgeVariant } from '../../../../../shared/utils/estado-badge.util';
import { prioridadBadgeVariant } from '../../../../../shared/utils/prioridad-badge.util';
import { SolicitudView } from '../../solicitud-view.model';

@Component({
  selector: 'app-solicitud-card',
  imports: [Badge, DatePipe, LucidePaperclip, LucideTrash],
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

  protected onDelete(event: Event): void {
    event.stopPropagation();
    this.delete.emit();
  }
}
