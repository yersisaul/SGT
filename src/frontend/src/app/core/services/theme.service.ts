import { DOCUMENT, Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type Theme = 'dark' | 'light';

/** Misma clave que lee el script inline de index.html antes del primer pintado. */
const THEME_KEY = 'sgt_theme';
const THEME_COLOR: Record<Theme, string> = { dark: '#0b1220', light: '#f8fafc' };
const SWITCH_TRANSITION_MS = 250;

/**
 * Tema visual de la aplicación (oscuro por defecto). La fuente de verdad es el
 * atributo `data-theme` de <html>, sobre el que styles.css remapea los tokens.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  private readonly current = signal<Theme>(this.readInitialTheme());

  readonly theme = this.current.asReadonly();

  toggle(): void {
    this.setTheme(this.current() === 'dark' ? 'light' : 'dark');
  }

  setTheme(theme: Theme): void {
    this.current.set(theme);
    if (!this.isBrowser) return;

    const root = this.document.documentElement;
    root.classList.add('theme-switching');
    root.setAttribute('data-theme', theme);
    this.document.querySelector('meta[name="theme-color"]')?.setAttribute('content', THEME_COLOR[theme]);
    setTimeout(() => root.classList.remove('theme-switching'), SWITCH_TRANSITION_MS);

    try {
      localStorage.setItem(THEME_KEY, theme);
    } catch {
      // Almacenamiento no disponible (modo privado): el tema dura solo esta sesión.
    }
  }

  private readInitialTheme(): Theme {
    if (!this.isBrowser) return 'dark';
    return this.document.documentElement.getAttribute('data-theme') === 'light' ? 'light' : 'dark';
  }
}
