import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { Component, computed, input, output } from '@angular/core';

import { Badge } from '../../../../../shared/components/badge/badge';
import { estadoBadgeVariant } from '../../../../../shared/utils/estado-badge.util';
import { OrdenKanbanColumn, OrdenView } from '../../orden-view.model';
import { OrdenCard } from '../orden-card/orden-card';

export interface OrdenMovida {
  item: OrdenView;
  estadoDestinoId: string;
}

/**
 * Tablero Kanban de Orden agrupado por Estado real del backend. Tercera
 * instancia del mismo patrón de Solicitudes/Requerimientos: con tres usos
 * reales confirmados, generalizar a un componente compartido ya deja de ser
 * prematuro — queda documentado como refactor recomendado (no se hizo acá
 * para no tocar Solicitudes/Requerimientos ya verificados dentro de esta
 * misma fase; ver informe de entrega).
 */
@Component({
  selector: 'app-orden-kanban',
  imports: [DragDropModule, Badge, OrdenCard],
  templateUrl: './kanban.html',
  styleUrl: './kanban.css',
})
export class Kanban {
  readonly columns = input.required<OrdenKanbanColumn[]>();
  readonly canUpdate = input(false);
  readonly canDelete = input(false);

  readonly cardOpen = output<OrdenView>();
  readonly cardDelete = output<OrdenView>();
  readonly moved = output<OrdenMovida>();

  protected readonly estadoBadgeVariant = estadoBadgeVariant;

  protected readonly columnIds = computed(() => this.columns().map((column) => column.estadoId));

  protected onDrop(event: CdkDragDrop<OrdenView[]>, estadoDestinoId: string): void {
    if (event.previousContainer === event.container) {
      return;
    }
    const item = event.item.data as OrdenView;
    this.moved.emit({ item, estadoDestinoId });
  }
}
