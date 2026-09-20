import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { Component, computed, input, output } from '@angular/core';

import { Badge } from '../../../../../shared/components/badge/badge';
import { estadoBadgeVariant } from '../../../../../shared/utils/estado-badge.util';
import { KanbanColumn, SolicitudView } from '../../solicitud-view.model';
import { SolicitudCard } from '../solicitud-card/solicitud-card';

export interface SolicitudMovida {
  item: SolicitudView;
  estadoDestinoId: string;
}

/**
 * Tablero Kanban agrupado por Estado real del backend (no hardcodeado). El
 * drag & drop solo detecta el cruce entre columnas (cambio de estado); el
 * reordenamiento dentro de una misma columna no se persiste porque el
 * backend no tiene un campo de orden para Solicitud.
 */
@Component({
  selector: 'app-solicitud-kanban',
  imports: [DragDropModule, Badge, SolicitudCard],
  templateUrl: './kanban.html',
  styleUrl: './kanban.css',
})
export class Kanban {
  readonly columns = input.required<KanbanColumn[]>();
  readonly canUpdate = input(false);
  readonly canDelete = input(false);

  readonly cardOpen = output<SolicitudView>();
  readonly cardDelete = output<SolicitudView>();
  readonly moved = output<SolicitudMovida>();

  protected readonly estadoBadgeVariant = estadoBadgeVariant;

  protected readonly columnIds = computed(() => this.columns().map((column) => column.estadoId));

  protected onDrop(event: CdkDragDrop<SolicitudView[]>, estadoDestinoId: string): void {
    if (event.previousContainer === event.container) {
      return;
    }
    const item = event.item.data as SolicitudView;
    this.moved.emit({ item, estadoDestinoId });
  }
}
