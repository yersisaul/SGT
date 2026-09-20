import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideActivity,
  LucideBoxes,
  LucideClipboardList,
  LucideClock,
  LucideKeyRound,
  LucideLayers,
  LucideListChecks,
  LucideShieldCheck,
  LucideTrendingUp,
  LucideUsers,
} from '@lucide/angular';

import { AuthService } from '../../../core/auth/auth.service';
import { EstadoCantidad } from '../../../core/models/dashboard.model';
import { OrdenResponse } from '../../../core/models/orden.model';
import { RequerimientoResponse } from '../../../core/models/requerimiento.model';
import { SolicitudResponse } from '../../../core/models/solicitud.model';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { OrdenService } from '../../../core/services/orden.service';
import { RequerimientoService } from '../../../core/services/requerimiento.service';
import { SolicitudService } from '../../../core/services/solicitud.service';
import { Badge } from '../../../shared/components/badge/badge';
import { Card } from '../../../shared/components/card/card';
import { StatusSummary } from '../../../shared/components/status-summary/status-summary';
import { estadoBadgeVariant } from '../../../shared/utils/estado-badge.util';
import { prioridadBadgeVariant } from '../../../shared/utils/prioridad-badge.util';

type AdminIcon = 'usuarios' | 'roles' | 'permisos' | 'activos' | 'especialidades' | 'estados';

interface AdminAtajo {
  label: string;
  route: string;
  icon: AdminIcon;
  permission: string;
}

interface ActividadItem {
  fecha: string;
  texto: string;
  route: string;
}

const ADMIN_ATAJOS: AdminAtajo[] = [
  { label: 'Usuarios', route: '/app/administracion/usuarios', icon: 'usuarios', permission: 'usuario.read' },
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

function estaFinalizado(nombreEstado: string): boolean {
  return nombreEstado.toLowerCase() === 'finalizado';
}

function contarPorEstado(nombres: string[]): EstadoCantidad[] {
  const conteo = new Map<string, number>();
  for (const nombre of nombres) {
    conteo.set(nombre, (conteo.get(nombre) ?? 0) + 1);
  }
  return Array.from(conteo.entries()).map(([estado, cantidad]) => ({ estado, cantidad }));
}

/**
 * Dashboard orientado al Administrador (Observación 1 del pasted_content).
 * Todas las métricas se calculan en cliente a partir de las listas completas
 * reales (Solicitud/Requerimiento/Orden — endpoints sin paginar), nunca de
 * datos inventados: cada número acá tiene una columna/campo real detrás.
 * `solicitudService.listar()`/`requerimientoService.listar()` respetan el
 * mismo filtrado por rol que el resto de la app (p. ej. Cliente solo ve sus
 * propias Solicitudes), a diferencia de los antiguos endpoints
 * resumen-estados que contaban sobre toda la tabla sin filtrar — por eso ya
 * no se usan acá.
 */
@Component({
  selector: 'app-dashboard',
  imports: [
    DatePipe,
    DecimalPipe,
    RouterLink,
    Card,
    Badge,
    StatusSummary,
    LucideUsers,
    LucideShieldCheck,
    LucideKeyRound,
    LucideBoxes,
    LucideLayers,
    LucideListChecks,
    LucideClipboardList,
    LucideClock,
    LucideTrendingUp,
    LucideActivity,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private readonly authService = inject(AuthService);
  private readonly solicitudService = inject(SolicitudService);
  private readonly requerimientoService = inject(RequerimientoService);
  private readonly ordenService = inject(OrdenService);
  private readonly catalogoService = inject(CatalogoService);

  readonly user = this.authService.user;

  protected readonly canViewSolicitudes = this.authService.hasPermission('solicitud.read');
  protected readonly canViewRequerimientos = this.authService.hasPermission('requerimiento.read');
  protected readonly canViewOrdenes = this.authService.hasPermission('orden.read');

  protected readonly adminAtajos = computed(() =>
    ADMIN_ATAJOS.filter((atajo) => this.authService.hasPermission(atajo.permission)),
  );

  protected readonly tieneResumenes = computed(
    () => this.canViewSolicitudes || this.canViewRequerimientos || this.canViewOrdenes,
  );

  private readonly nombrePorIdEstado = signal<Map<string, string>>(new Map());

  protected readonly solicitudesLoading = signal(false);
  protected readonly solicitudesError = signal<string | null>(null);
  private readonly solicitudes = signal<SolicitudResponse[]>([]);

  protected readonly requerimientosLoading = signal(false);
  protected readonly requerimientosError = signal<string | null>(null);
  private readonly requerimientos = signal<RequerimientoResponse[]>([]);

  protected readonly ordenesLoading = signal(false);
  protected readonly ordenesError = signal<string | null>(null);
  private readonly ordenes = signal<OrdenResponse[]>([]);

  // --- Estado actual del flujo (reutiliza app-status-summary existente) ---
  protected readonly solicitudesTotal = computed(() => this.solicitudes().length);
  protected readonly solicitudesItems = computed(() =>
    contarPorEstado(this.solicitudes().map((s) => this.nombreEstado(s.id_estado))),
  );

  protected readonly requerimientosTotal = computed(() => this.requerimientos().length);
  protected readonly requerimientosItems = computed(() =>
    contarPorEstado(this.requerimientos().map((r) => this.nombreEstado(r.id_estado))),
  );

  protected readonly ordenesTotal = computed(() => this.ordenes().length);
  protected readonly ordenesItems = computed(() =>
    contarPorEstado(this.ordenes().map((o) => this.nombreEstado(o.id_estado))),
  );

  // --- Resumen ejecutivo (KPIs) ---
  protected readonly kpiSolicitudesActivas = computed(
    () => this.solicitudes().filter((s) => !estaFinalizado(this.nombreEstado(s.id_estado))).length,
  );
  protected readonly kpiRequerimientosPendientes = computed(
    () =>
      this.requerimientos().filter((r) => {
        const nombre = this.nombreEstado(r.id_estado).toLowerCase();
        return nombre === 'pendiente' || nombre === 'en revisión';
      }).length,
  );
  protected readonly kpiOrdenesActivas = computed(() => this.ordenes().filter((o) => !o.fecha_cierre).length);
  protected readonly kpiOrdenesFinalizadas = computed(() => this.ordenes().filter((o) => !!o.fecha_cierre).length);

  // --- Elementos que requieren atención ---
  protected readonly solicitudesPorClasificar = computed(
    () => this.solicitudes().filter((s) => this.nombreEstado(s.id_estado).toLowerCase() === 'pendiente').length,
  );
  protected readonly ordenesPorIniciar = computed(
    () => this.ordenes().filter((o) => this.nombreEstado(o.id_estado).toLowerCase() === 'pendiente').length,
  );
  protected readonly tieneAtencion = computed(
    () =>
      this.kpiRequerimientosPendientes() > 0 ||
      this.solicitudesPorClasificar() > 0 ||
      this.ordenesPorIniciar() > 0,
  );

  // --- Distribución y actividad ---
  protected readonly solicitudesPorPrioridad = computed(() => {
    const conteo = new Map<string, number>();
    for (const s of this.solicitudes()) {
      conteo.set(s.prioridad, (conteo.get(s.prioridad) ?? 0) + 1);
    }
    return Array.from(conteo.entries()).map(([prioridad, cantidad]) => ({ prioridad, cantidad }));
  });

  protected readonly ordenesOrigenSolicitud = computed(() => this.ordenes().filter((o) => !!o.id_solicitud).length);
  protected readonly ordenesOrigenRequerimiento = computed(
    () => this.ordenes().filter((o) => !!o.id_requerimiento).length,
  );

  /** Tiempo promedio de cierre real (fecha_cierre - fecha_registro) sobre las
   * Órdenes efectivamente cerradas. null si todavía no hay ninguna cerrada
   * (evita mostrar un promedio sin datos reales detrás). */
  protected readonly tiempoPromedioCierreHoras = computed<number | null>(() => {
    const cerradas = this.ordenes().filter((o) => !!o.fecha_cierre);
    if (cerradas.length === 0) return null;
    const totalHoras = cerradas.reduce((acumulado, orden) => {
      const inicio = new Date(orden.fecha_registro).getTime();
      const fin = new Date(orden.fecha_cierre as string).getTime();
      return acumulado + (fin - inicio) / (1000 * 60 * 60);
    }, 0);
    return totalHoras / cerradas.length;
  });

  protected readonly actividadReciente = computed<ActividadItem[]>(() => {
    const items: ActividadItem[] = [
      ...this.solicitudes().map((s) => ({
        fecha: s.fecha_registro,
        texto: `Solicitud ${s.numeroSolicitud} registrada`,
        route: '/app/solicitudes',
      })),
      ...this.requerimientos().map((r) => ({
        fecha: r.fecha_registro,
        texto: `Requerimiento ${r.numeroRequerimiento} registrado`,
        route: '/app/requerimientos',
      })),
      ...this.ordenes().map((o) => ({
        fecha: o.fecha_registro,
        texto: `Orden ${o.numeroOrden} generada`,
        route: '/app/ordenes',
      })),
      ...this.ordenes()
        .filter((o) => !!o.fecha_cierre)
        .map((o) => ({
          fecha: o.fecha_cierre as string,
          texto: `Orden ${o.numeroOrden} cerrada`,
          route: '/app/ordenes',
        })),
    ];
    return items.sort((a, b) => b.fecha.localeCompare(a.fecha)).slice(0, 8);
  });

  protected readonly estadoBadgeVariant = estadoBadgeVariant;
  protected readonly prioridadBadgeVariant = prioridadBadgeVariant;

  constructor() {
    this.catalogoService.getEstados().subscribe((estados) => {
      this.nombrePorIdEstado.set(new Map(estados.map((e) => [e.id_estado, e.nombre])));
    });

    if (this.canViewSolicitudes) {
      this.loadSolicitudes();
    }
    if (this.canViewRequerimientos) {
      this.loadRequerimientos();
    }
    if (this.canViewOrdenes) {
      this.loadOrdenes();
    }
  }

  protected loadSolicitudes(): void {
    this.solicitudesLoading.set(true);
    this.solicitudesError.set(null);
    this.solicitudService.listar().subscribe({
      next: (data) => {
        this.solicitudes.set(data);
        this.solicitudesLoading.set(false);
      },
      error: () => {
        this.solicitudesError.set('No se pudo cargar el resumen de solicitudes.');
        this.solicitudesLoading.set(false);
      },
    });
  }

  protected loadRequerimientos(): void {
    this.requerimientosLoading.set(true);
    this.requerimientosError.set(null);
    this.requerimientoService.listar().subscribe({
      next: (data) => {
        this.requerimientos.set(data);
        this.requerimientosLoading.set(false);
      },
      error: () => {
        this.requerimientosError.set('No se pudo cargar el resumen de requerimientos.');
        this.requerimientosLoading.set(false);
      },
    });
  }

  protected loadOrdenes(): void {
    this.ordenesLoading.set(true);
    this.ordenesError.set(null);
    this.ordenService.listar().subscribe({
      next: (data) => {
        this.ordenes.set(data);
        this.ordenesLoading.set(false);
      },
      error: () => {
        this.ordenesError.set('No se pudo cargar el resumen de órdenes.');
        this.ordenesLoading.set(false);
      },
    });
  }

  private nombreEstado(idEstado: string): string {
    return this.nombrePorIdEstado().get(idEstado) ?? 'Desconocido';
  }
}
