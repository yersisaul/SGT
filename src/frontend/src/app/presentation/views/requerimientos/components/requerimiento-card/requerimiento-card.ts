import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { LucidePaperclip, LucideTrash } from '@lucide/angular';

import { Badge } from '../../../../../shared/components/badge/badge';
import { estadoBadgeVariant } from '../../../../../shared/utils/estado-badge.util';
import { RequerimientoView } from '../../requerimiento-view.model';

@Component({
  selector: 'app-requerimiento-card',
  imports: [Badge, DatePipe, LucidePaperclip, LucideTrash],
  templateUrl: './requerimiento-card.html',
  styleUrl: './requerimiento-card.css',
})
export class RequerimientoCard {
  readonly item = input.required<RequerimientoView>();
  readonly canDelete = input(false);

  readonly open = output<void>();
  readonly delete = output<void>();

  protected readonly estadoBadgeVariant = estadoBadgeVariant;

  protected onDelete(event: Event): void {
    event.stopPropagation();
    this.delete.emit();
  }
}
