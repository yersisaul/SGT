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

  readonly items = this.notifications.asReadonly();

  show(message: string, variant: NotificationVariant = 'info'): void {
    const id = this.nextId++;
    this.notifications.update((current) => [...current, { id, message, variant }]);
    setTimeout(() => this.dismiss(id), AUTO_DISMISS_MS);
  }

  success(message: string): void {
    this.show(message, 'success');
  }

  error(message: string): void {
    this.show(message, 'error');
  }

  dismiss(id: number): void {
    this.notifications.update((current) => current.filter((n) => n.id !== id));
  }
}
