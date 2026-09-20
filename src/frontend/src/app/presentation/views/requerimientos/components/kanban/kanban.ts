import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { Component, computed, input, output } from '@angular/core';

import { Badge } from '../../../../../shared/components/badge/badge';
import { estadoBadgeVariant } from '../../../../../shared/utils/estado-badge.util';
import { RequerimientoKanbanColumn, RequerimientoView } from '../../requerimiento-view.model';
import { RequerimientoCard } from '../requerimiento-card/requerimiento-card';

export interface RequerimientoMovido {
  item: RequerimientoView;
  estadoDestinoId: string;
}

/**
 * Tablero Kanban de Requerimiento agrupado por Estado real del backend.
 * Mismo patrón que solicitudes/components/kanban (no se generalizó a un
 * componente compartido todavía: con solo dos usos reales — Solicitud y
 * este — preferí no abstraer prematuramente; se revisa cuando exista un
 * tercer caso real con Órdenes).
 */
@Component({
  selector: 'app-requerimiento-kanban',
  imports: [DragDropModule, Badge, RequerimientoCard],
  templateUrl: './kanban.html',
  styleUrl: './kanban.css',
})
export class Kanban {
  readonly columns = input.required<RequerimientoKanbanColumn[]>();
  readonly canUpdate = input(false);
  readonly canDelete = input(false);

  readonly cardOpen = output<RequerimientoView>();
  readonly cardDelete = output<RequerimientoView>();
  readonly moved = output<RequerimientoMovido>();

  protected readonly estadoBadgeVariant = estadoBadgeVariant;

  protected readonly columnIds = computed(() => this.columns().map((column) => column.estadoId));

  protected onDrop(event: CdkDragDrop<RequerimientoView[]>, estadoDestinoId: string): void {
    if (event.previousContainer === event.container) {
      return;
    }
    const item = event.item.data as RequerimientoView;
    this.moved.emit({ item, estadoDestinoId });
  }
}
