import { Component, input } from '@angular/core';

@Component({
  selector: 'app-spinner',
  templateUrl: './spinner.html',
  styleUrl: './spinner.css',
})
export class Spinner {
  readonly size = input<'sm' | 'md'>('md');
  /** 'inverted' se usa sobre fondos oscuros/de color (ej. botón primario). */
  readonly tone = input<'default' | 'inverted'>('default');
  readonly label = input('Cargando');
}
