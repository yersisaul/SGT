import { Component, computed, inject, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {
  LucideBoxes,
  LucideClipboardCheck,
  LucideClipboardList,
  LucideKeyRound,
  LucideLayers,
  LucideLayoutDashboard,
  LucideListChecks,
  LucideLogOut,
  LucideShieldCheck,
  LucideUsers,
  LucideWrench,
} from '@lucide/angular';

import { AuthService } from '../../../core/auth/auth.service';
import { NotificacionStreamService } from '../../../core/services/notificacion-stream.service';

type NavIcon =
  | 'dashboard'
  | 'solicitudes'
  | 'requerimientos'
  | 'ordenes'
  | 'usuarios'
  | 'roles'
  | 'permisos'
  | 'activos'
  | 'especialidades'
  | 'estados';

interface NavItem {
  label: string;
  route: string;
  icon: NavIcon;
  /** Permiso requerido para ver el ítem. Sin permiso definido = visible para cualquier usuario autenticado. */
  permission?: string;
}

/**
 * Catálogo de navegación del SGT. Cada módulo (Solicitudes, Requerimientos,
 * Órdenes, Usuarios, etc.) agrega aquí su entrada cuando su página exista —
 * evita enlaces a rutas que todavía no están implementadas.
 */
const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', route: '/app/dashboard', icon: 'dashboard' },
  {
    label: 'Solicitudes',
    route: '/app/solicitudes',
    icon: 'solicitudes',
    permission: 'solicitud.read',
  },
  {
    label: 'Requerimientos',
    route: '/app/requerimientos',
    icon: 'requerimientos',
    permission: 'requerimiento.read',
  },
  { label: 'Órdenes', route: '/app/ordenes', icon: 'ordenes', permission: 'orden.read' },
];

/**
 * Sección "Administración" (catálogos/configuración): cada ítem se muestra
 * únicamente si el usuario tiene el permiso .read correspondiente — nunca por
 * nombre de rol.
 */
const ADMIN_NAV_ITEMS: NavItem[] = [
  // usuario.read ya no es exclusivo de quien administra usuarios (Cliente y
  // Despachador lo tienen para resolver nombres, ver app.routes.ts); el
  // enlace del menú se gatea con usuario.create, que solo tiene Administrador.
  { label: 'Usuarios', route: '/app/administracion/usuarios', icon: 'usuarios', permission: 'usuario.create' },
  { label: 'Roles', route: '/app/administracion/roles', icon: 'roles', permission: 'rol.read' },
  { label: 'Permisos', route: '/app/administracion/permisos', icon: 'permisos', permission: 'permiso.read' },
  { label: 'Activos', route: '/app/administracion/activos', icon: 'activos', permission: 'activo.read' },
  {
    label: 'Especialidades',
    route: '/app/administracion/especialidades',
    icon: 'especialidades',
    permission: 'especialidad.read',
  },
  { label: 'Estados', route: '/app/administracion/estados', icon: 'estados', permission: 'estado.read' },
];

@Component({
  selector: 'app-sidebar',
  imports: [
    RouterLink,
    RouterLinkActive,
    LucideLayoutDashboard,
    LucideClipboardList,
    LucideClipboardCheck,
    LucideWrench,
    LucideLogOut,
    LucideUsers,
    LucideShieldCheck,
    LucideKeyRound,
    LucideBoxes,
    LucideLayers,
    LucideListChecks,
  ],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {
  private readonly authService = inject(AuthService);
  private readonly stream = inject(NotificacionStreamService);

  /** OT en mi cola + asignadas sin confirmar (se actualiza por SSE, FR-028). */
  readonly pendientesOrdenes = this.stream.pendientes;

  readonly open = input(false);
  readonly navigate = output<void>();

  readonly navItems = computed(() =>
    NAV_ITEMS.filter((item) => !item.permission || this.authService.hasPermission(item.permission)),
  );

  readonly adminNavItems = computed(() =>
    ADMIN_NAV_ITEMS.filter((item) => !item.permission || this.authService.hasPermission(item.permission)),
  );

  logout(): void {
    this.authService.logout();
  }

  onNavigate(): void {
    this.navigate.emit();
  }
}
