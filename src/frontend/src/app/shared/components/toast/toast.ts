import { Component, inject } from '@angular/core';
import { LucideX } from '../../icons/lucide-icons';

import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-toast',
  imports: [LucideX],
  templateUrl: './toast.html',
  styleUrl: './toast.css',
})
export class Toast {
  private readonly notifications = inject(NotificationService);

  readonly items = this.notifications.items;

  dismiss(id: number): void {
    this.notifications.dismiss(id);
  }
}
