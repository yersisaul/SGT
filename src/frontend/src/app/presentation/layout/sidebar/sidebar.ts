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
  LucideMoon,
  LucideShieldCheck,
  LucideSun,
  LucideUsers,
  LucideWrench,
} from '../../../shared/icons/lucide-icons';

import { AuthService } from '../../../core/auth/auth.service';
import { ModuloOperativo } from '../../../core/auth/permisos-base';
import { NotificacionStreamService } from '../../../core/services/notificacion-stream.service';
import { APP_VERSION } from '../../../core/config/app-version';
import { ThemeService } from '../../../core/services/theme.service';
import { BrandLogo } from '../../../shared/components/brand-logo/brand-logo';

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
  /** Módulo operativo: visible si el usuario puede actuar sobre él (AuthService.puedeOperarModulo). */
  modulo?: ModuloOperativo;
}

/**
 * Catálogo de navegación del SGT. Cada módulo (Solicitudes, Requerimientos,
 * Órdenes, Usuarios, etc.) agrega aquí su entrada cuando su página exista —
 * evita enlaces a rutas que todavía no están implementadas.
 */
const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', route: '/app/dashboard', icon: 'dashboard' },
  // Todos los roles tienen el .read de estos módulos (LECTURA_BASE): el menú
  // aparece solo a quien puede crear, editar, eliminar u operar sobre ellos.
  { label: 'Solicitudes', route: '/app/solicitudes', icon: 'solicitudes', modulo: 'solicitudes' },
  { label: 'Requerimientos', route: '/app/requerimientos', icon: 'requerimientos', modulo: 'requerimientos' },
  { label: 'Órdenes', route: '/app/ordenes', icon: 'ordenes', modulo: 'ordenes' },
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
    LucideSun,
    LucideMoon,
    BrandLogo,
  ],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {
  private readonly authService = inject(AuthService);
  private readonly stream = inject(NotificacionStreamService);
  private readonly themeService = inject(ThemeService);

  /** OT en mi cola + asignadas sin confirmar (se actualiza por SSE, FR-028). */
  readonly pendientesOrdenes = this.stream.pendientes;

  readonly open = input(false);
  readonly navigate = output<void>();

  readonly navItems = computed(() =>
    NAV_ITEMS.filter((item) =>
      item.modulo
        ? this.authService.puedeOperarModulo(item.modulo)
        : !item.permission || this.authService.hasPermission(item.permission),
    ),
  );

  readonly adminNavItems = computed(() =>
    ADMIN_NAV_ITEMS.filter((item) => !item.permission || this.authService.hasPermission(item.permission)),
  );

  readonly version = APP_VERSION;

  readonly isDark = computed(() => this.themeService.theme() === 'dark');

  toggleTheme(): void {
    this.themeService.toggle();
  }

  logout(): void {
    this.authService.logout();
  }

  onNavigate(): void {
    this.navigate.emit();
  }
}
