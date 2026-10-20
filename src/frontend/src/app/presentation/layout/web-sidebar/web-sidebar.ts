import { Component, computed, inject, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';
import { APP_VERSION } from '../../../core/config/app-version';
import { ThemeService } from '../../../core/services/theme.service';
import { BrandLogo } from '../../../shared/components/brand-logo/brand-logo';
import {
  LucideArrowLeft,
  LucideAward,
  LucideBriefcase,
  LucideBuilding2,
  LucideFileText,
  LucideHandshake,
  LucideLayoutDashboard,
  LucideLogOut,
  LucideMoon,
  LucideNewspaper,
  LucidePackage,
  LucideSun,
  LucideUserCheck,
} from '../../../shared/icons/lucide-icons';

type WebNavIcon =
  | 'resumen'
  | 'productos'
  | 'servicios'
  | 'partners'
  | 'clientes'
  | 'casos'
  | 'novedades'
  | 'documentos'
  | 'registros';

interface WebNavItem {
  label: string;
  route: string;
  icon: WebNavIcon;
  permission?: string;
}

interface WebNavSeccion {
  titulo: string;
  items: WebNavItem[];
}

/**
 * Menú del panel del sitio web, agrupado como el sitio: catálogo, prueba
 * social, contenido y cuentas. Cada ítem se muestra solo con su permiso.
 */
const SECCIONES: WebNavSeccion[] = [
  { titulo: 'General', items: [{ label: 'Resumen', route: '/web/resumen', icon: 'resumen' }] },
  {
    titulo: 'Catálogo',
    items: [
      { label: 'Productos', route: '/web/productos', icon: 'productos', permission: 'web_catalogo.read' },
      { label: 'Servicios', route: '/web/servicios', icon: 'servicios', permission: 'web_catalogo.read' },
    ],
  },
  {
    titulo: 'Comercial',
    items: [
      { label: 'Casos de éxito', route: '/web/casos-exito', icon: 'casos', permission: 'web_comercial.read' },
      { label: 'Clientes', route: '/web/clientes', icon: 'clientes', permission: 'web_comercial.read' },
      { label: 'Partners', route: '/web/partners', icon: 'partners', permission: 'web_comercial.read' },
    ],
  },
  {
    titulo: 'Contenido',
    items: [
      { label: 'Novedades', route: '/web/novedades', icon: 'novedades', permission: 'web_novedad.read' },
      { label: 'Brochures', route: '/web/documentos', icon: 'documentos', permission: 'web_documento.read' },
    ],
  },
  {
    titulo: 'Cuentas',
    items: [{ label: 'Registros web', route: '/web/registros', icon: 'registros', permission: 'usuario.update' }],
  },
];

@Component({
  selector: 'app-web-sidebar',
  imports: [
    RouterLink,
    RouterLinkActive,
    BrandLogo,
    LucideArrowLeft,
    LucideAward,
    LucideBriefcase,
    LucideBuilding2,
    LucideFileText,
    LucideHandshake,
    LucideLayoutDashboard,
    LucideLogOut,
    LucideMoon,
    LucideNewspaper,
    LucidePackage,
    LucideSun,
    LucideUserCheck,
  ],
  templateUrl: './web-sidebar.html',
  // Mismo aspecto que el menú del SGT: se reutilizan sus estilos tal cual.
  styleUrls: ['../sidebar/sidebar.css', './web-sidebar.css'],
})
export class WebSidebar {
  private readonly authService = inject(AuthService);
  private readonly themeService = inject(ThemeService);

  readonly open = input(false);
  readonly navigate = output<void>();

  readonly secciones = computed(() =>
    SECCIONES.map((seccion) => ({
      ...seccion,
      items: seccion.items.filter((item) => !item.permission || this.authService.hasPermission(item.permission)),
    })).filter((seccion) => seccion.items.length > 0),
  );

  /** El enlace de vuelta al SGT solo si el usuario también opera allí. */
  readonly puedeIrAlSgt = computed(() => this.authService.permisos().some((permiso) => !permiso.startsWith('web_')));

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
