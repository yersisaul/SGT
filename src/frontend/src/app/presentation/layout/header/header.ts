import { Component, computed, inject, output } from '@angular/core';
import { LucideBell, LucideMenu } from '@lucide/angular';

import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-header',
  imports: [LucideBell, LucideMenu],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  private readonly authService = inject(AuthService);

  readonly toggleSidebar = output<void>();

  readonly user = this.authService.user;
  readonly initials = computed(() => {
    const user = this.user();
    if (!user) return '';
    return `${user.nombres.charAt(0)}${user.apellidos.charAt(0)}`.toUpperCase();
  });
}
