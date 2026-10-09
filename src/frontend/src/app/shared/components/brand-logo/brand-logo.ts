import { Component, computed, input } from '@angular/core';

/**
 * Logo de CFBD. `variant`: 'full' incluye el lema, 'mark' solo el isotipo.
 * `surface`: 'auto' alterna la versión según el tema activo (por CSS, sin
 * parpadeo con SSR); 'dark' fuerza la versión clara para fondos siempre
 * oscuros, como el sidebar.
 */
@Component({
  selector: 'app-brand-logo',
  templateUrl: './brand-logo.html',
  styleUrl: './brand-logo.css',
})
export class BrandLogo {
  readonly variant = input<'full' | 'mark'>('mark');
  readonly surface = input<'auto' | 'dark'>('auto');

  protected readonly onDarkSrc = computed(() => `/brand/cfbd-${this.variant()}-dark.png`);
  protected readonly onLightSrc = computed(() => `/brand/cfbd-${this.variant()}-light.png`);
  /** Proporciones reales de los PNG: reservan espacio y evitan saltos de layout. */
  protected readonly size = computed(() =>
    this.variant() === 'full' ? { w: 640, h: 298 } : { w: 320, h: 125 },
  );
}
