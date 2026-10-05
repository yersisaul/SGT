import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideCircleAlert, LucideInbox, LucideLayoutGrid, LucideLock, LucideTable } from '@lucide/angular';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AuthService } from '../../../core/auth/auth.service';
import { EspecialidadCatalogo, EstadoCatalogo } from '../../../core/models/catalogo.model';
import { MiEspecialidadResponse, MiembroEspecialidadResponse } from '../../../core/models/especialidad.model';
import {
  AsignacionOrdenResponse,
  CargaMiembroResponse,
  HistorialOrdenResponse,
  OrdenResponse,
  TipoAsignacionOrden,
} from '../../../core/models/orden.model';
import { ArchivoService } from '../../../core/services/archivo.service';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { EspecialidadService } from '../../../core/services/especialidad.service';
import { NotificacionStreamService } from '../../../core/services/notificacion-stream.service';
import { OrdenService } from '../../../core/services/orden.service';
import { RequerimientoService } from '../../../core/services/requerimiento.service';
import { SolicitudService } from '../../../core/services/solicitud.service';
import { Badge } from '../../../shared/components/badge/badge';
import { Button } from '../../../shared/components/button/button';
import { Dialog } from '../../../shared/components/dialog/dialog';
import { Select, SelectOption } from '../../../shared/components/select/select';
import { Spinner } from '../../../shared/components/spinner/spinner';
import { NotificationService } from '../../../shared/services/notification.service';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';
import { estadoBadgeVariant } from '../../../shared/utils/estado-badge.util';
import { estadoOrderRank } from '../../../shared/utils/estado-order.util';
import { Ejecutar, EjecutarResultado } from './components/ejecutar/ejecutar';
import { Kanban } from './components/kanban/kanban';
import { Tabla } from './components/tabla/tabla';
import {
  esEstadoAsignada,
  esEstadoDevuelta,
  esEstadoEnProgreso,
  esEstadoFinalizado,
  esEstadoPendiente,
  esEstadoValidoDeOrden,
} from './orden-estados.config';
import { OrdenKanbanColumn, OrdenView, OrigenContexto } from './orden-view.model';

type ViewMode = 'kanban' | 'tabla';
/** Bandejas del flujo (PRD FR-044/FR-045). */
type Bandeja = 'cola' | 'mias' | 'equipo' | 'todas';

const ETIQUETA_ASIGNACION: Record<TipoAsignacionOrden, string> = {
  ENCOLADA: 'Encolada',
  TOMADA: 'Tomada de la cola',
  ASIGNADA: 'Asignada por el responsable',
  CONFIRMADA: 'Confirmada por el ejecutor',
  DEVUELTA: 'Devuelta al responsable',
  REASIGNADA_ESPECIALIDAD: 'Reasignada de especialidad',
};

@Component({
  selector: 'app-ordenes',
  imports: [
    DatePipe,
    FormsModule,
    Badge,
    Button,
    Dialog,
    Select,
    Spinner,
    Kanban,
    Tabla,
    Ejecutar,
    LucideLayoutGrid,
    LucideTable,
    LucideInbox,
    LucideCircleAlert,
    LucideLock,
  ],
  templateUrl: './ordenes.html',
  styleUrl: './ordenes.css',
})
export class Ordenes {
  private readonly authService = inject(AuthService);
  private readonly ordenService = inject(OrdenService);
  private readonly archivoService = inject(ArchivoService);
  private readonly solicitudService = inject(SolicitudService);
  private readonly requerimientoService = inject(RequerimientoService);
  private readonly especialidadService = inject(EspecialidadService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly notifications = inject(NotificationService);
  private readonly stream = inject(NotificacionStreamService);

  protected readonly canUpdate = this.authService.hasPermission('orden.update');
  protected readonly canCerrar = this.authService.hasPermission('orden.cerrar');
  protected readonly canDelete = this.authService.hasPermission('orden.delete');
  protected readonly canReasignar = this.authService.hasPermission('orden.reasignar');
  protected readonly canTomar = this.authService.hasPermission('orden.tomar');
  protected readonly canAsignar = this.authService.hasPermission('orden.asignar');
  protected readonly canVerificar = this.authService.hasPermission('orden.verificar');
  protected readonly canVerTodo = this.authService.hasPermission('orden.read_all');
  protected readonly canReadHistorial = this.authService.hasPermission('historial_orden.read');
  protected readonly canReadSolicitud = this.authService.hasPermission('solicitud.read');
  protected readonly canReadRequerimiento = this.authService.hasPermission('requerimiento.read');

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);

  private readonly ordenes = signal<OrdenResponse[]>([]);
  private readonly cola = signal<OrdenResponse[]>([]);
  private readonly estados = signal<EstadoCatalogo[]>([]);
  private readonly especialidades = signal<EspecialidadCatalogo[]>([]);
  private readonly misEspecialidades = signal<MiEspecialidadResponse[]>([]);

  protected readonly viewMode = signal<ViewMode>('kanban');
  protected readonly bandeja = signal<Bandeja>('todas');

  protected readonly estadosOrden = computed(() => this.estados().filter((e) => esEstadoValidoDeOrden(e.nombre)));
  protected readonly estadosDisponibles = computed(() => this.estadosOrden().length > 0);

  private readonly idsMiembro = computed(() => new Set(this.misEspecialidades().map((e) => e.id_especialidad)));
  private readonly idsResponsable = computed(
    () => new Set(this.misEspecialidades().filter((e) => e.es_responsable).map((e) => e.id_especialidad)),
  );
  protected readonly esResponsable = computed(() => this.idsResponsable().size > 0);
  protected readonly especialidadesResponsable = computed(() =>
    this.misEspecialidades().filter((e) => e.es_responsable),
  );

  protected readonly bandejasDisponibles = computed<{ id: Bandeja; etiqueta: string }[]>(() => {
    const lista: { id: Bandeja; etiqueta: string }[] = [];
    if (this.canTomar && this.misEspecialidades().length > 0) {
      lista.push({ id: 'cola', etiqueta: `Cola (${this.cola().length})` });
    }
    if (this.canTomar) {
      lista.push({ id: 'mias', etiqueta: `Mis órdenes (${this.misOrdenesAbiertas().length})` });
    }
    if (this.esResponsable()) {
      lista.push({ id: 'equipo', etiqueta: 'Mi equipo' });
    }
    lista.push({ id: 'todas', etiqueta: this.canVerTodo ? 'Todas' : 'Visibles para mí' });
    return lista;
  });

  private readonly misOrdenesAbiertas = computed(() => {
    const yo = this.authService.user()?.id;
    return this.ordenes().filter((o) => o.id_usuario === yo && !o.fecha_cierre);
  });

  // ---- Diálogos ----
  protected readonly dialogTarget = signal<OrdenResponse | null>(null);
  protected readonly ejecutarSubmitting = signal(false);
  protected readonly accionSubmitting = signal(false);

  protected readonly confirmTarget = signal<OrdenResponse | null>(null);
  protected readonly deleteSubmitting = signal(false);

  protected readonly cerrarTarget = signal<OrdenResponse | null>(null);
  protected readonly cerrarComentario = signal('');
  protected readonly cerrarSubmitting = signal(false);
  protected readonly cerrarError = signal<string | null>(null);

  protected readonly reasignarTarget = signal<OrdenResponse | null>(null);
  protected readonly reasignarEspecialidad = signal('');
  protected readonly reasignarMotivo = signal('');
  protected readonly reasignarSubmitting = signal(false);
  protected readonly reasignarError = signal<string | null>(null);

  protected readonly asignarTarget = signal<OrdenResponse | null>(null);
  protected readonly asignarMiembros = signal<MiembroEspecialidadResponse[]>([]);
  protected readonly asignarSeleccionado = signal('');
  protected readonly asignarSubmitting = signal(false);
  protected readonly asignarError = signal<string | null>(null);

  protected readonly devolverTarget = signal<OrdenResponse | null>(null);
  protected readonly devolverMotivo = signal('');
  protected readonly devolverSubmitting = signal(false);
  protected readonly devolverError = signal<string | null>(null);

  protected readonly historial = signal<HistorialOrdenResponse[]>([]);
  protected readonly historialLoading = signal(false);
  protected readonly asignaciones = signal<AsignacionOrdenResponse[]>([]);

  protected readonly origen = signal<OrigenContexto>(null);
  protected readonly origenLoading = signal(false);

  // ---- Equipo (responsable) ----
  protected readonly equipoEspecialidad = signal('');
  protected readonly carga = signal<CargaMiembroResponse[]>([]);
  protected readonly cargaLoading = signal(false);

  protected readonly estadoBadgeVariant = estadoBadgeVariant;

  private readonly todasLasViews = computed<OrdenView[]>(() => this.ordenes().map((raw) => this.toView(raw)));

  protected readonly views = computed<OrdenView[]>(() => {
    switch (this.bandeja()) {
      case 'cola':
        return this.cola().map((raw) => this.toView(raw));
      case 'mias':
        return this.misOrdenesAbiertas().map((raw) => this.toView(raw));
      case 'equipo': {
        const id = this.equipoEspecialidad();
        return this.todasLasViews().filter((v) => v.raw.id_especialidad === id && !v.raw.fecha_cierre);
      }
      default:
        return this.todasLasViews();
    }
  });

  protected readonly kanbanColumns = computed<OrdenKanbanColumn[]>(() => {
    const agrupado = new Map<string, OrdenView[]>();
    for (const view of this.views()) {
      const lista = agrupado.get(view.raw.id_estado) ?? [];
      lista.push(view);
      agrupado.set(view.raw.id_estado, lista);
    }
    return [...this.estadosOrden()]
      .sort((a, b) => estadoOrderRank(a.nombre) - estadoOrderRank(b.nombre))
      .map((estado) => ({
        estadoId: estado.id_estado,
        estadoNombre: estado.nombre,
        items: agrupado.get(estado.id_estado) ?? [],
        // Solo informativo: el tablero no permite arrastrar (la OT avanza con acciones).
        transicionable: !esEstadoFinalizado(estado.nombre),
      }));
  });

  protected readonly dialogView = computed<OrdenView | null>(() => {
    const target = this.dialogTarget();
    return target ? this.toView(target) : null;
  });

  protected readonly dialogEstaCerrada = computed(() => !!this.dialogTarget()?.fecha_cierre);

  // Acciones disponibles: curación de UI; el backend vuelve a validar cada regla.
  protected readonly acciones = computed(() => {
    const view = this.dialogView();
    const yo = this.authService.user()?.id;
    if (!view || view.raw.fecha_cierre) {
      return { tomar: false, asignar: false, verificar: false, reasignar: false, cerrar: false, informe: false };
    }
    const esp = view.raw.id_especialidad;
    const sinEjecutor = !view.raw.id_usuario;
    const soyEjecutor = !!yo && view.raw.id_usuario === yo;
    const soyResponsable = this.idsResponsable().has(esp);
    return {
      tomar: this.canTomar && sinEjecutor && esEstadoPendiente(view.estadoNombre) && this.idsMiembro().has(esp),
      asignar:
        this.canAsignar &&
        sinEjecutor &&
        soyResponsable &&
        (esEstadoPendiente(view.estadoNombre) || esEstadoDevuelta(view.estadoNombre)),
      verificar: this.canVerificar && soyEjecutor && esEstadoAsignada(view.estadoNombre),
      reasignar: this.canReasignar && (soyResponsable || this.canVerTodo),
      cerrar: this.canCerrar && soyEjecutor && esEstadoEnProgreso(view.estadoNombre),
      informe: this.canUpdate && soyEjecutor && esEstadoEnProgreso(view.estadoNombre),
    };
  });

  protected readonly especialidadDestinoOptions = computed<SelectOption[]>(() => {
    const actual = this.reasignarTarget()?.id_especialidad;
    return this.especialidades()
      .filter((e) => e.id_especialidad !== actual)
      .map((e) => ({ value: e.id_especialidad, label: e.nombre }));
  });

  protected readonly miembroOptions = computed<SelectOption[]>(() =>
    this.asignarMiembros().map((m) => ({
      value: m.id_usuario,
      label: `${m.nombres} ${m.apellidos}${m.es_responsable ? ' (responsable)' : ''}`,
    })),
  );

  protected readonly equipoOptions = computed<SelectOption[]>(() =>
    this.especialidadesResponsable().map((e) => ({ value: e.id_especialidad, label: e.nombre })),
  );

  constructor() {
    this.loadAll();

    effect(() => {
      if (!this.loading() && !this.estadosDisponibles() && this.viewMode() === 'kanban') {
        this.viewMode.set('tabla');
      }
    });

    // Aviso en tiempo real (orden.encolada / asignada / devuelta): refrescar
    // las bandejas sin recargar la página (FR-028/FR-029).
    effect(() => {
      if (this.stream.ultimoEvento()) {
        this.refrescarBandejas();
      }
    });
  }

  protected reload(): void {
    this.loadAll();
  }

  protected cambiarBandeja(bandeja: Bandeja): void {
    this.bandeja.set(bandeja);
    if (bandeja === 'equipo') {
      if (!this.equipoEspecialidad() && this.especialidadesResponsable().length > 0) {
        this.equipoEspecialidad.set(this.especialidadesResponsable()[0].id_especialidad);
      }
      this.loadCarga();
    }
  }

  protected cambiarEquipo(idEspecialidad: string): void {
    this.equipoEspecialidad.set(idEspecialidad);
    this.loadCarga();
  }

  protected openDetail(view: OrdenView): void {
    this.dialogTarget.set(view.raw);
    this.loadDetalle(view.raw);
  }

  protected closeDialog(): void {
    this.dialogTarget.set(null);
    this.historial.set([]);
    this.asignaciones.set([]);
    this.origen.set(null);
  }

  // ---- Paso 10: tomar de la cola ----
  protected tomar(orden: OrdenResponse): void {
    this.accionSubmitting.set(true);
    this.ordenService.tomar(orden.id_orden).subscribe({
      next: (actualizada) => this.aplicarCambio(actualizada, `Tomaste la orden ${actualizada.numeroOrden}.`),
      error: () => {
        this.accionSubmitting.set(false);
        this.refrescarBandejas();
      },
    });
  }

  // ---- Paso 11: verificar si corresponde ----
  protected confirmarCorresponde(orden: OrdenResponse): void {
    this.accionSubmitting.set(true);
    this.ordenService.verificar(orden.id_orden, { corresponde: true }).subscribe({
      next: (actualizada) =>
        this.aplicarCambio(actualizada, `Orden ${actualizada.numeroOrden} en progreso.`),
      error: () => this.accionSubmitting.set(false),
    });
  }

  protected requestDevolver(orden: OrdenResponse): void {
    this.devolverTarget.set(orden);
    this.devolverMotivo.set('');
    this.devolverError.set(null);
  }

  protected cancelDevolver(): void {
    if (this.devolverSubmitting()) return;
    this.devolverTarget.set(null);
  }

  protected confirmDevolverAction(): void {
    const target = this.devolverTarget();
    const motivo = this.devolverMotivo().trim();
    if (!target || !motivo) return;
    this.devolverSubmitting.set(true);
    this.devolverError.set(null);
    this.ordenService.verificar(target.id_orden, { corresponde: false, motivo }).subscribe({
      next: (actualizada) => {
        this.devolverSubmitting.set(false);
        this.devolverTarget.set(null);
        this.aplicarCambio(actualizada, `Orden ${actualizada.numeroOrden} devuelta al responsable.`);
      },
      error: (error: unknown) => {
        this.devolverSubmitting.set(false);
        this.devolverError.set(extractApiErrorMessage(error));
      },
    });
  }

  // ---- Responsable: asignar a un miembro ----
  protected requestAsignar(orden: OrdenResponse): void {
    this.asignarTarget.set(orden);
    this.asignarSeleccionado.set('');
    this.asignarError.set(null);
    this.asignarMiembros.set([]);
    this.especialidadService.miembros(orden.id_especialidad).subscribe({
      next: (miembros) => this.asignarMiembros.set(miembros),
      error: (error: unknown) => this.asignarError.set(extractApiErrorMessage(error)),
    });
  }

  protected cancelAsignar(): void {
    if (this.asignarSubmitting()) return;
    this.asignarTarget.set(null);
  }

  protected confirmAsignarAction(): void {
    const target = this.asignarTarget();
    const idUsuario = this.asignarSeleccionado();
    if (!target || !idUsuario) return;
    this.asignarSubmitting.set(true);
    this.asignarError.set(null);
    this.ordenService.asignar(target.id_orden, { id_usuario: idUsuario }).subscribe({
      next: (actualizada) => {
        this.asignarSubmitting.set(false);
        this.asignarTarget.set(null);
        this.aplicarCambio(actualizada, `Orden ${actualizada.numeroOrden} asignada a ${actualizada.nombre_ejecutor}.`);
        this.loadCarga();
      },
      error: (error: unknown) => {
        this.asignarSubmitting.set(false);
        this.asignarError.set(extractApiErrorMessage(error));
      },
    });
  }

  // ---- Paso 12: reasignar a otra especialidad ----
  protected requestReasignar(orden: OrdenResponse): void {
    this.reasignarTarget.set(orden);
    this.reasignarEspecialidad.set('');
    this.reasignarMotivo.set('');
    this.reasignarError.set(null);
  }

  protected cancelReasignar(): void {
    if (this.reasignarSubmitting()) return;
    this.reasignarTarget.set(null);
    this.reasignarError.set(null);
  }

  protected confirmReasignarAction(): void {
    const target = this.reasignarTarget();
    const destino = this.reasignarEspecialidad();
    const motivo = this.reasignarMotivo().trim();
    if (!target || !destino || !motivo) return;

    this.reasignarSubmitting.set(true);
    this.reasignarError.set(null);
    this.ordenService.reasignar(target.id_orden, { id_especialidad_destino: destino, motivo }).subscribe({
      next: (actualizada) => {
        this.reasignarSubmitting.set(false);
        this.reasignarTarget.set(null);
        this.aplicarCambio(
          actualizada,
          `Orden ${actualizada.numeroOrden} reasignada a ${this.nombreEspecialidad(destino)}.`,
        );
      },
      error: (error: unknown) => {
        this.reasignarSubmitting.set(false);
        this.reasignarError.set(extractApiErrorMessage(error));
      },
    });
  }

  // ---- Pasos 13-14: informe y cierre ----
  protected handleEjecutar(resultado: EjecutarResultado): void {
    const original = this.dialogTarget();
    if (!original) return;
    const terminar = (final: OrdenResponse) => {
      this.ejecutarSubmitting.set(false);
      this.reemplazar(final);
      this.dialogTarget.set(final);
    };
    this.ejecutarSubmitting.set(true);
    if (resultado.archivo) {
      this.archivoService.subir<OrdenResponse>('ordenes', original.id_orden, resultado.archivo).subscribe({
        next: (final) => {
          terminar(final);
          this.notifications.success('Informe técnico guardado.');
        },
        error: () => {
          this.notifications.error('No se pudo subir el informe técnico.');
          terminar(original);
        },
      });
      return;
    }
    if (resultado.eliminarArchivo) {
      this.archivoService.eliminar<OrdenResponse>('ordenes', original.id_orden).subscribe({
        next: terminar,
        error: () => terminar(original),
      });
      return;
    }
    terminar(original);
  }

  protected requestCerrar(orden: OrdenResponse): void {
    this.cerrarTarget.set(orden);
    this.cerrarComentario.set('');
    this.cerrarError.set(null);
  }

  protected cancelCerrar(): void {
    if (this.cerrarSubmitting()) return;
    this.cerrarTarget.set(null);
    this.cerrarError.set(null);
  }

  protected confirmCerrarAction(): void {
    const target = this.cerrarTarget();
    if (!target) return;

    this.cerrarSubmitting.set(true);
    this.cerrarError.set(null);
    this.ordenService.cerrar(target.id_orden, { comentario: this.cerrarComentario().trim() || undefined }).subscribe({
      next: (cerrada) => {
        this.cerrarSubmitting.set(false);
        this.cerrarTarget.set(null);
        this.aplicarCambio(cerrada, `Orden ${cerrada.numeroOrden} cerrada correctamente.`);
      },
      error: (error: unknown) => {
        this.cerrarSubmitting.set(false);
        this.cerrarError.set(extractApiErrorMessage(error));
      },
    });
  }

  /** El adjunto exige JWT (Authorization header): se descarga vía HttpClient y se abre como blob. */
  protected verAdjunto(url: string | null): void {
    if (!url) return;
    this.archivoService.descargarBlob(url).subscribe((blob) => {
      const objectUrl = URL.createObjectURL(blob);
      window.open(objectUrl, '_blank');
      setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
    });
  }

  protected requestDelete(orden: OrdenResponse): void {
    this.confirmTarget.set(orden);
  }

  protected cancelDelete(): void {
    this.confirmTarget.set(null);
  }

  protected confirmDeleteAction(): void {
    const target = this.confirmTarget();
    if (!target) return;

    this.deleteSubmitting.set(true);
    this.ordenService.eliminar(target.id_orden).subscribe({
      next: () => {
        this.ordenes.update((lista) => lista.filter((o) => o.id_orden !== target.id_orden));
        this.deleteSubmitting.set(false);
        this.confirmTarget.set(null);
        if (this.dialogTarget()?.id_orden === target.id_orden) {
          this.closeDialog();
        }
        this.notifications.success('Orden eliminada correctamente.');
      },
      error: () => this.deleteSubmitting.set(false),
    });
  }

  protected estadoNombreHistorial(idEstado: string): string {
    return this.buscarNombre(this.estados(), idEstado, (e) => e.id_estado, (e) => e.nombre, 'Desconocido');
  }

  protected etiquetaAsignacion(asignacion: AsignacionOrdenResponse): string {
    const base = ETIQUETA_ASIGNACION[asignacion.tipo];
    if (asignacion.tipo === 'REASIGNADA_ESPECIALIDAD' && asignacion.id_especialidad_origen) {
      return `${base}: ${this.nombreEspecialidad(asignacion.id_especialidad_origen)} → ${this.nombreEspecialidad(
        asignacion.id_especialidad_destino,
      )}`;
    }
    return base;
  }

  private aplicarCambio(actualizada: OrdenResponse, mensaje: string): void {
    this.accionSubmitting.set(false);
    this.reemplazar(actualizada);
    if (this.dialogTarget()?.id_orden === actualizada.id_orden) {
      this.dialogTarget.set(actualizada);
      this.loadDetalle(actualizada);
    }
    this.notifications.success(mensaje);
    this.refrescarBandejas();
  }

  private reemplazar(actualizada: OrdenResponse): void {
    this.ordenes.update((lista) => {
      const existe = lista.some((o) => o.id_orden === actualizada.id_orden);
      return existe
        ? lista.map((o) => (o.id_orden === actualizada.id_orden ? actualizada : o))
        : [actualizada, ...lista];
    });
  }

  private refrescarBandejas(): void {
    this.ordenService.listar().subscribe({ next: (ordenes) => this.ordenes.set(ordenes), error: () => undefined });
    if (this.canTomar && this.misEspecialidades().length > 0) {
      this.ordenService.cola().subscribe({ next: (cola) => this.cola.set(cola), error: () => undefined });
    }
  }

  private loadDetalle(orden: OrdenResponse): void {
    this.loadHistorial(orden.id_orden);
    this.ordenService.asignaciones(orden.id_orden).subscribe({
      next: (registros) => this.asignaciones.set([...registros].reverse()),
      error: () => this.asignaciones.set([]),
    });
    this.loadOrigen(orden);
  }

  private loadCarga(): void {
    const id = this.equipoEspecialidad();
    if (!id) return;
    this.cargaLoading.set(true);
    this.ordenService.cargaEquipo(id).subscribe({
      next: (carga) => {
        this.carga.set(carga);
        this.cargaLoading.set(false);
      },
      error: () => this.cargaLoading.set(false),
    });
  }

  private loadHistorial(idOrden: string): void {
    if (!this.canReadHistorial) return;
    this.historialLoading.set(true);
    this.ordenService.listarHistorial(idOrden).subscribe({
      next: (registros) => {
        this.historial.set([...registros].sort((a, b) => b.fecha.localeCompare(a.fecha)));
        this.historialLoading.set(false);
      },
      error: () => this.historialLoading.set(false),
    });
  }

  /** Origen: Solicitud o Requerimiento según cuál de los dos ids viene con valor. */
  private loadOrigen(orden: OrdenResponse): void {
    this.origen.set(null);
    if (orden.id_solicitud) {
      if (!this.canReadSolicitud) {
        this.origen.set({ tipo: 'sin-acceso' });
        return;
      }
      this.origenLoading.set(true);
      this.solicitudService.obtener(orden.id_solicitud).subscribe({
        next: (data) => {
          this.origen.set({ tipo: 'solicitud', data });
          this.origenLoading.set(false);
        },
        error: () => {
          this.origen.set({ tipo: 'sin-acceso' });
          this.origenLoading.set(false);
        },
      });
    } else if (orden.id_requerimiento) {
      if (!this.canReadRequerimiento) {
        this.origen.set({ tipo: 'sin-acceso' });
        return;
      }
      this.origenLoading.set(true);
      this.requerimientoService.obtener(orden.id_requerimiento).subscribe({
        next: (data) => {
          this.origen.set({ tipo: 'requerimiento', data });
          this.origenLoading.set(false);
        },
        error: () => {
          this.origen.set({ tipo: 'sin-acceso' });
          this.origenLoading.set(false);
        },
      });
    }
  }

  private loadAll(): void {
    this.loading.set(true);
    this.loadError.set(null);

    forkJoin({
      ordenes: this.ordenService.listar(),
      estados: this.catalogoService.getEstados(),
      especialidades: this.catalogoService.getEspecialidades(),
      mias: this.canTomar ? this.especialidadService.mias().pipe(catchError(() => of([]))) : of([]),
    }).subscribe({
      next: ({ ordenes, estados, especialidades, mias }) => {
        this.ordenes.set(ordenes);
        this.estados.set(estados);
        this.especialidades.set(especialidades);
        this.misEspecialidades.set(mias);
        this.loading.set(false);
        // Operaciones arranca en su cola; el resto, en el listado general.
        if (this.canTomar && mias.length > 0) {
          this.bandeja.set('cola');
          this.refrescarBandejas();
        }
      },
      error: () => {
        this.loadError.set('No se pudieron cargar las órdenes.');
        this.loading.set(false);
      },
    });
  }

  private toView(raw: OrdenResponse): OrdenView {
    return {
      raw,
      estadoNombre: this.buscarNombre(this.estados(), raw.id_estado, (e) => e.id_estado, (e) => e.nombre, 'Desconocido'),
      especialidadNombre: this.nombreEspecialidad(raw.id_especialidad),
      usuarioNombre: raw.nombre_ejecutor ?? 'En cola (sin ejecutor)',
    };
  }

  private nombreEspecialidad(id: string): string {
    return this.buscarNombre(this.especialidades(), id, (e) => e.id_especialidad, (e) => e.nombre, '—');
  }

  private buscarNombre<T>(
    lista: T[],
    id: string,
    getId: (item: T) => string,
    getNombre: (item: T) => string,
    fallback: string,
  ): string {
    const encontrado = lista.find((item) => getId(item) === id);
    return encontrado ? getNombre(encontrado) : fallback;
  }
}
