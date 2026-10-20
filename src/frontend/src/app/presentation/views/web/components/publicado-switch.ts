import { Component, input, output } from '@angular/core';

/** Interruptor Publicado/Oculto de una fila del panel (PATCH /{id}/publicacion). */
@Component({
  selector: 'app-publicado-switch',
  template: `
    <button
      type="button"
      role="switch"
      class="switch"
      [class.switch-on]="publicado()"
      [attr.aria-checked]="publicado()"
      [attr.aria-label]="(publicado() ? 'Ocultar ' : 'Publicar ') + etiqueta()"
      [disabled]="disabled() || cargando()"
      (click)="$event.stopPropagation(); cambiar.emit(!publicado())"
    >
      <span class="switch-pista" aria-hidden="true"><span class="switch-punto"></span></span>
      <span class="switch-texto">{{ publicado() ? 'Publicado' : 'Oculto' }}</span>
    </button>
  `,
  styles: `
    .switch {
      display: inline-flex;
      align-items: center;
      gap: var(--space-2);
      background: none;
      border: none;
      padding: 4px 0;
      font-family: var(--font-sans);
      font-size: 12px;
      font-weight: 500;
      color: var(--color-subtle-foreground);
      cursor: pointer;
    }
    .switch:disabled {
      cursor: default;
      opacity: 0.6;
    }
    .switch-pista {
      position: relative;
      width: 30px;
      height: 18px;
      border-radius: 999px;
      background: var(--status-neutral-bg);
      border: 1px solid var(--color-border);
      transition: background-color 150ms ease;
    }
    .switch-punto {
      position: absolute;
      top: 2px;
      left: 2px;
      width: 12px;
      height: 12px;
      border-radius: 50%;
      background: var(--color-subtle-foreground);
      transition: transform 150ms ease, background-color 150ms ease;
    }
    .switch-on {
      color: var(--status-success-fg);
    }
    .switch-on .switch-pista {
      background: var(--status-success-bg);
      border-color: transparent;
    }
    .switch-on .switch-punto {
      transform: translateX(12px);
      background: var(--status-success-fg);
    }
    .switch:focus-visible {
      outline: 2px solid var(--color-accent);
      outline-offset: 2px;
      border-radius: var(--radius-sm);
    }
    @media (prefers-reduced-motion: reduce) {
      .switch-pista,
      .switch-punto {
        transition: none;
      }
    }
  `,
})
export class PublicadoSwitch {
  readonly publicado = input.required<boolean>();
  readonly etiqueta = input('');
  readonly disabled = input(false);
  readonly cargando = input(false);
  readonly cambiar = output<boolean>();
}
