import { Component, input, model } from '@angular/core';

import { LucidePlus, LucideX } from '../../../../shared/icons/lucide-icons';

/** Editor de una lista de textos cortos (características de productos y servicios). */
@Component({
  selector: 'app-lista-texto',
  imports: [LucidePlus, LucideX],
  template: `
    <div class="field">
      <span class="field-label">{{ label() }}</span>
      @if (items().length === 0) {
        <p class="vacio">{{ vacio() }}</p>
      }
      <ol class="lista">
        @for (item of items(); track $index; let i = $index) {
          <li class="fila">
            <span class="numero" aria-hidden="true">{{ i + 1 }}</span>
            <input
              class="input"
              type="text"
              [value]="item"
              [attr.maxlength]="maxLength()"
              [attr.aria-label]="label() + ' ' + (i + 1)"
              [disabled]="disabled()"
              (input)="editar(i, $any($event.target).value)"
            />
            <button type="button" class="quitar" [disabled]="disabled()" [attr.aria-label]="'Quitar ' + (i + 1)" (click)="quitar(i)">
              <svg lucideX [size]="14" aria-hidden="true"></svg>
            </button>
          </li>
        }
      </ol>
      @if (items().length < max()) {
        <button type="button" class="agregar" [disabled]="disabled()" (click)="agregar()">
          <svg lucidePlus [size]="14" aria-hidden="true"></svg>
          {{ textoAgregar() }}
        </button>
      }
    </div>
  `,
  styleUrl: './web-controles.css',
})
export class ListaTexto {
  readonly label = input('Elementos');
  readonly textoAgregar = input('Agregar');
  readonly vacio = input('Sin elementos.');
  readonly max = input(30);
  readonly maxLength = input(300);
  readonly disabled = input(false);
  readonly items = model<string[]>([]);

  protected agregar(): void {
    this.items.update((lista) => [...lista, '']);
  }

  protected editar(indice: number, texto: string): void {
    this.items.update((lista) => lista.map((item, i) => (i === indice ? texto : item)));
  }

  protected quitar(indice: number): void {
    this.items.update((lista) => lista.filter((_, i) => i !== indice));
  }
}
