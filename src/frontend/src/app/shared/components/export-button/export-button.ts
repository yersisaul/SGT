import { Component, inject, input, signal } from '@angular/core';
import { Observable } from 'rxjs';

import { LucideDownload } from '../../icons/lucide-icons';
import { NotificationService } from '../../services/notification.service';
import { guardarBlob, nombreCsvDelDia } from '../../utils/descarga.util';
import { Button } from '../button/button';

/**
 * "Descargar CSV" de un listado. El backend arma el archivo con lo que el
 * usuario puede ver (misma regla de visibilidad que el listado).
 */
@Component({
  selector: 'app-export-button',
  imports: [Button, LucideDownload],
  template: `
    <app-button variant="secondary" [loading]="descargando()" (click)="descargar()">
      @if (!descargando()) {
        <svg lucideDownload [size]="16" aria-hidden="true"></svg>
      }
      Descargar CSV
    </app-button>
  `,
})
export class ExportButton {
  private readonly notifications = inject(NotificationService);

  /** Petición que devuelve el CSV (p. ej. solicitudService.exportar()). */
  readonly fuente = input.required<() => Observable<Blob>>();
  /** Prefijo del nombre del archivo: "<prefijo>-AAAA-MM-DD.csv". */
  readonly prefijo = input.required<string>();

  protected readonly descargando = signal(false);

  protected descargar(): void {
    if (this.descargando()) return;
    this.descargando.set(true);
    this.fuente()().subscribe({
      next: (blob) => {
        guardarBlob(blob, nombreCsvDelDia(this.prefijo()));
        this.descargando.set(false);
      },
      error: () => {
        this.notifications.error('No se pudo descargar el archivo.');
        this.descargando.set(false);
      },
    });
  }
}
