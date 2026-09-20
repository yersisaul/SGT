import { Component, input } from '@angular/core';

export type BadgeVariant = 'neutral' | 'info' | 'success' | 'warning' | 'danger';

@Component({
  selector: 'app-badge',
  templateUrl: './badge.html',
  styleUrl: './badge.css',
})
export class Badge {
  readonly variant = input<BadgeVariant>('neutral');
}
