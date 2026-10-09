import { Injectable, signal } from '@angular/core';

export type NotificationVariant = 'success' | 'error' | 'info';

export interface Notification {
  id: number;
  message: string;
  variant: NotificationVariant;
}

const AUTO_DISMISS_MS = 5000;

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private nextId = 0;
  private readonly notifications = signal<Notification[]>([]);
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();

  readonly items = this.notifications.asReadonly();

  /**
   * Un mismo aviso (mensaje + tipo) no se apila: si ya está visible solo se
   * reinicia su temporizador. Evita la pila de errores idénticos cuando varias
   * peticiones fallan a la vez (p. ej. servidor caído).
   */
  show(message: string, variant: NotificationVariant = 'info'): void {
    const existing = this.notifications().find((n) => n.message === message && n.variant === variant);
    const id = existing?.id ?? this.nextId++;
    if (!existing) {
      this.notifications.update((current) => [...current, { id, message, variant }]);
    }
    clearTimeout(this.timers.get(id));
    this.timers.set(id, setTimeout(() => this.dismiss(id), AUTO_DISMISS_MS));
  }

  success(message: string): void {
    this.show(message, 'success');
  }

  error(message: string): void {
    this.show(message, 'error');
  }

  dismiss(id: number): void {
    clearTimeout(this.timers.get(id));
    this.timers.delete(id);
    this.notifications.update((current) => current.filter((n) => n.id !== id));
  }
}
