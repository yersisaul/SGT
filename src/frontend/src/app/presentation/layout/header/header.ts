import { Component, computed, inject, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideBell, LucideMenu } from '../../../shared/icons/lucide-icons';

import { AuthService } from '../../../core/auth/auth.service';
import { NotificacionStreamService } from '../../../core/services/notificacion-stream.service';

@Component({
  selector: 'app-header',
  imports: [RouterLink, LucideBell, LucideMenu],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  private readonly authService = inject(AuthService);
  private readonly stream = inject(NotificacionStreamService);

  readonly toggleSidebar = output<void>();

  readonly user = this.authService.user;

  /** La campana lleva a Órdenes: solo si el menú Órdenes está visible. */
  readonly puedeVerOrdenes = computed(() => this.authService.puedeOperarModulo('ordenes'));
  /** Mismo contador que el badge del menú (cola + asignadas sin confirmar, vía SSE). */
  readonly pendientes = this.stream.pendientes;
  readonly bellLabel = computed(() => {
    const total = this.pendientes();
    if (total === 0) return 'Notificaciones: sin órdenes pendientes';
    return `Notificaciones: ${total} ${total === 1 ? 'orden pendiente' : 'órdenes pendientes'} de acción`;
  });
  readonly initials = computed(() => {
    const user = this.user();
    if (!user) return '';
    return `${user.nombres.charAt(0)}${user.apellidos.charAt(0)}`.toUpperCase();
  });
}
