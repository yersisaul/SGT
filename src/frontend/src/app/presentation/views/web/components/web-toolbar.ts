import { Component, input, model } from '@angular/core';

import { LucideSearch } from '../../../../shared/icons/lucide-icons';
import { FiltroPublicacion } from '../seccion-web.base';

/** Búsqueda + filtro Todos/Publicados/Ocultos de las secciones del panel web. */
@Component({
  selector: 'app-web-toolbar',
  imports: [LucideSearch],
  template: `
    <div class="admin-toolbar">
      <div class="admin-search">
        <svg lucideSearch [size]="16" aria-hidden="true"></svg>
        <input
          type="search"
          [placeholder]="placeholder()"
          [value]="busqueda()"
          (input)="busqueda.set($any($event.target).value)"
          [attr.aria-label]="placeholder()"
        />
      </div>
      @if (conFiltro()) {
        <div class="web-filtro" role="group" aria-label="Filtrar por publicación">
          @for (opcion of opciones; track opcion.id) {
            <button type="button" [attr.aria-pressed]="filtro() === opcion.id" (click)="filtro.set(opcion.id)">
              {{ opcion.etiqueta }}<span class="web-filtro-conteo">{{ conteo()[opcion.id] }}</span>
            </button>
          }
        </div>
      }
    </div>
  `,
  styleUrl: './web-controles.css',
})
export class WebToolbar {
  readonly placeholder = input('Buscar…');
  readonly conteo = input<Record<FiltroPublicacion, number>>({ todos: 0, publicados: 0, ocultos: 0 });
  readonly conFiltro = input(true);
  readonly busqueda = model('');
  readonly filtro = model<FiltroPublicacion>('todos');

  protected readonly opciones: { id: FiltroPublicacion; etiqueta: string }[] = [
    { id: 'todos', etiqueta: 'Todos' },
    { id: 'publicados', etiqueta: 'Publicados' },
    { id: 'ocultos', etiqueta: 'Ocultos' },
  ];
}
