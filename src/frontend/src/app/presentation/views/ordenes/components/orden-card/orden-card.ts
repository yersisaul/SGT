import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { LucidePaperclip, LucideTrash } from '../../../../../shared/icons/lucide-icons';

import { Badge } from '../../../../../shared/components/badge/badge';
import { estadoBadgeVariant } from '../../../../../shared/utils/estado-badge.util';
import { OrdenView } from '../../orden-view.model';

@Component({
  selector: 'app-orden-card',
  imports: [Badge, DatePipe, LucidePaperclip, LucideTrash],
  templateUrl: './orden-card.html',
  styleUrl: './orden-card.css',
})
export class OrdenCard {
  readonly item = input.required<OrdenView>();
  readonly canDelete = input(false);

  readonly open = output<void>();
  readonly delete = output<void>();

  protected readonly estadoBadgeVariant = estadoBadgeVariant;

  private static readonly MS_POR_DIA = 1000 * 60 * 60 * 24;
  private static readonly DIAS_ALERTA = 3;

  /** Antigüedad desde fecha_registro — ayuda a priorizar OTs que llevan
   * tiempo abiertas (instrucción de UX: tiempo/antigüedad), sin depender de
   * ningún campo que el backend no tenga. */
  protected get diasEnCurso(): number {
    const inicio = new Date(this.item().raw.fecha_registro).getTime();
    return Math.max(0, Math.floor((Date.now() - inicio) / OrdenCard.MS_POR_DIA));
  }

  protected get esAntigua(): boolean {
    return !this.item().raw.fecha_cierre && this.diasEnCurso >= OrdenCard.DIAS_ALERTA;
  }

  protected onDelete(event: Event): void {
    event.stopPropagation();
    this.delete.emit();
  }
}
