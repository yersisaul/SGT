import { Component, HostListener, input, output } from '@angular/core';

export type DialogVariant = 'modal' | 'drawer';

/**
 * Overlay compartido con dos variantes: 'modal' (centrado, para creación y
 * confirmaciones) y 'drawer' (panel lateral, para detalle/edición sin perder
 * el contexto del tablero de fondo). Un solo componente evita mantener dos
 * primitivas de overlay separadas.
 */
@Component({
  selector: 'app-dialog',
  templateUrl: './dialog.html',
  styleUrl: './dialog.css',
})
export class Dialog {
  readonly open = input(false);
  readonly variant = input<DialogVariant>('modal');
  readonly title = input('');
  readonly closed = output<void>();

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    if (this.open()) {
      this.closed.emit();
    }
  }

  protected close(): void {
    this.closed.emit();
  }
}
