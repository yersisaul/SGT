import { DatePipe } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideArrowUpDown, LucideChevronLeft, LucideChevronRight, LucideSearch, LucideTrash } from '@lucide/angular';

import { Badge } from '../../../../../shared/components/badge/badge';
import { Button } from '../../../../../shared/components/button/button';
import { estadoBadgeVariant } from '../../../../../shared/utils/estado-badge.util';
import { prioridadBadgeVariant } from '../../../../../shared/utils/prioridad-badge.util';
import { SolicitudView } from '../../solicitud-view.model';

type SortKey = 'numero' | 'prioridad' | 'fecha';

const PAGE_SIZE = 10;

/**
 * Vista de tabla sobre la misma lista que consume el Kanban (GET
 * /api/solicitudes no pagina ni filtra server-side, así que búsqueda/orden/
 * paginación se resuelven en frontend sobre la lista ya cargada).
 */
@Component({
  selector: 'app-solicitud-tabla',
  imports: [FormsModule, DatePipe, Badge, Button, LucideSearch, LucideArrowUpDown, LucideChevronLeft, LucideChevronRight, LucideTrash],
  templateUrl: './tabla.html',
  styleUrl: './tabla.css',
})
export class Tabla {
  readonly items = input.required<SolicitudView[]>();
  readonly canDelete = input(false);

  readonly open = output<SolicitudView>();
  readonly delete = output<SolicitudView>();

  protected readonly estadoBadgeVariant = estadoBadgeVariant;
  protected readonly prioridadBadgeVariant = prioridadBadgeVariant;

  protected readonly searchTerm = signal('');
  protected readonly estadoFiltro = signal('');
  protected readonly sortKey = signal<SortKey>('fecha');
  protected readonly sortAsc = signal(false);
  protected readonly page = signal(1);
  protected readonly pageSize = PAGE_SIZE;

  protected readonly estadosDisponibles = computed(() => {
    const nombres = new Set(this.items().map((item) => item.estadoNombre));
    return Array.from(nombres).sort();
  });

  private readonly filtrados = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const estado = this.estadoFiltro();
    return this.items().filter((item) => {
      const matchesTerm =
        !term ||
        item.raw.numeroSolicitud.toLowerCase().includes(term) ||
        item.raw.descripcion.toLowerCase().includes(term);
      const matchesEstado = !estado || item.estadoNombre === estado;
      return matchesTerm && matchesEstado;
    });
  });

  protected readonly ordenados = computed(() => {
    const key = this.sortKey();
    const asc = this.sortAsc();
    const copia = [...this.filtrados()];
    copia.sort((a, b) => {
      let comparison = 0;
      if (key === 'numero') {
        comparison = a.raw.numeroSolicitud.localeCompare(b.raw.numeroSolicitud);
      } else if (key === 'prioridad') {
        comparison = a.raw.prioridad.localeCompare(b.raw.prioridad);
      } else {
        comparison = a.raw.fecha_registro.localeCompare(b.raw.fecha_registro);
      }
      return asc ? comparison : -comparison;
    });
    return copia;
  });

  protected readonly totalPaginas = computed(() => Math.max(1, Math.ceil(this.ordenados().length / this.pageSize)));

  protected readonly paginados = computed(() => {
    const paginaActual = Math.min(this.page(), this.totalPaginas());
    const start = (paginaActual - 1) * this.pageSize;
    return this.ordenados().slice(start, start + this.pageSize);
  });

  protected setSearch(value: string): void {
    this.searchTerm.set(value);
    this.page.set(1);
  }

  protected setEstadoFiltro(value: string): void {
    this.estadoFiltro.set(value);
    this.page.set(1);
  }

  protected toggleSort(key: SortKey): void {
    if (this.sortKey() === key) {
      this.sortAsc.set(!this.sortAsc());
    } else {
      this.sortKey.set(key);
      this.sortAsc.set(true);
    }
  }

  protected nextPage(): void {
    this.page.set(Math.min(this.page() + 1, this.totalPaginas()));
  }

  protected prevPage(): void {
    this.page.set(Math.max(this.page() - 1, 1));
  }
}
