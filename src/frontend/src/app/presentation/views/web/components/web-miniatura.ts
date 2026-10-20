import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';

import { ArchivoService } from '../../../../core/services/archivo.service';

/**
 * Miniatura de una imagen del panel (url_img/url_logo/url_portada). Esas
 * rutas exigen JWT, así que se descargan con HttpClient y se muestran como
 * blob local; sin imagen se muestran las iniciales del registro.
 */
@Component({
  selector: 'app-web-miniatura',
  template: `
    @if (src(); as imagen) {
      <img [src]="imagen" [alt]="alt()" [class.contener]="contener()" />
    } @else {
      <span class="iniciales" aria-hidden="true">{{ iniciales() }}</span>
    }
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      width: 44px;
      height: 44px;
      border-radius: var(--radius-md);
      background: var(--color-bg);
      border: 1px solid var(--color-border);
      overflow: hidden;
    }
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    img.contener {
      object-fit: contain;
      padding: 4px;
      background: #ffffff;
    }
    .iniciales {
      font-size: 13px;
      font-weight: 600;
      color: var(--color-subtle-foreground);
    }
  `,
})
export class WebMiniatura {
  private readonly archivoService = inject(ArchivoService);

  readonly url = input<string | null>(null);
  readonly nombre = input('');
  readonly alt = input('');
  /** Logos: se ven completos sobre blanco en vez de recortados. */
  readonly contener = input(false);

  protected readonly src = signal<string | null>(null);

  constructor() {
    effect((onCleanup) => {
      const url = this.url();
      this.src.set(null);
      if (!url) return;
      let objectUrl: string | null = null;
      const suscripcion = this.archivoService.descargarBlob(url).subscribe({
        next: (blob) => {
          objectUrl = URL.createObjectURL(blob);
          this.src.set(objectUrl);
        },
        error: () => this.src.set(null),
      });
      onCleanup(() => {
        suscripcion.unsubscribe();
        if (objectUrl) URL.revokeObjectURL(objectUrl);
      });
    });
    inject(DestroyRef).onDestroy(() => this.src.set(null));
  }

  protected iniciales(): string {
    return this.nombre()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((palabra) => palabra.charAt(0).toUpperCase())
      .join('');
  }
}
