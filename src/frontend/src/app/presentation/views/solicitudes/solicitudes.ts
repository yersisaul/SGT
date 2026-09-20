import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { LucideCircleAlert, LucideInbox, LucideLayoutGrid, LucidePlus, LucideTable } from '@lucide/angular';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import { ActivoCatalogo, EspecialidadCatalogo, EstadoCatalogo, UsuarioCatalogo } from '../../../core/models/catalogo.model';
import {
  HistorialSolicitudResponse,
  SolicitudRequest,
  SolicitudResponse,
} from '../../../core/models/solicitud.model';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { SolicitudService } from '../../../core/services/solicitud.service';
import { Button } from '../../../shared/components/button/button';
import { Dialog } from '../../../shared/components/dialog/dialog';
import { Spinner } from '../../../shared/components/spinner/spinner';
import { NotificationService } from '../../../shared/services/notification.service';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';
import { estadoOrderRank } from '../../../shared/utils/estado-order.util';
import { Formulario } from './components/formulario/formulario';
import { Kanban, SolicitudMovida } from './components/kanban/kanban';
import { Tabla } from './components/tabla/tabla';
import { esEstadoEditablePorPut, esEstadoValidoDeSolicitud } from './solicitud-estados.config';
import { KanbanColumn, SolicitudView } from './solicitud-view.model';

type ViewMode = 'kanban' | 'tabla';
type DialogMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-solicitudes',
  imports: [
    DatePipe,
    Button,
    Dialog,
    Spinner,
    Kanban,
    Tabla,
    Formulario,
    LucidePlus,
    LucideLayoutGrid,
    LucideTable,
    LucideInbox,
    LucideCircleAlert,
  ],
  templateUrl: './solicitudes.html',
  styleUrl: './solicitudes.css',
})
export class Solicitudes {
  private readonly authService = inject(AuthService);
  private readonly solicitudService = inject(SolicitudService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly notifications = inject(NotificationService);

  protected readonly canCreate = this.authService.hasPermission('solicitud.create');
  protected readonly canUpdate = this.authService.hasPermission('solicitud.update');
  protected readonly canDelete = this.authService.hasPermission('solicitud.delete');
  protected readonly canGenerarOrden = this.authService.hasPermission('solicitud.generar_orden');
  protected readonly canCreateRequerimiento = this.authService.hasPermission('requerimiento.create');
  protected readonly canReadHistorial = this.authService.hasPermission('historial_solicitud.read');

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);

  private readonly solicitudes = signal<SolicitudResponse[]>([]);
  private readonly estados = signal<EstadoCatalogo[]>([]);
  private readonly activos = signal<ActivoCatalogo[]>([]);
  private readonly especialidades = signal<EspecialidadCatalogo[]>([]);
  private readonly usuarios = signal<UsuarioCatalogo[]>([]);

  protected readonly viewMode = signal<ViewMode>('kanban');

  // Estados válidos para Solicitud del catálogo compartido /api/estados
  // (que también sirve a Requerimiento). Ver solicitud-estados.config.ts.
  protected readonly estadosSolicitud = computed(() => this.estados().filter((e) => esEstadoValidoDeSolicitud(e.nombre)));
  protected readonly estadosDisponibles = computed(() => this.estadosSolicitud().length > 0);

  protected readonly dialogMode = signal<DialogMode>(null);
  protected readonly dialogTarget = signal<SolicitudResponse | null>(null);
  protected readonly formSubmitting = signal(false);

  protected readonly confirmTarget = signal<SolicitudResponse | null>(null);
  protected readonly deleteSubmitting = signal(false);

  protected readonly generarOrdenTarget = signal<SolicitudResponse | null>(null);
  protected readonly generarOrdenSubmitting = signal(false);
  protected readonly generarOrdenError = signal<string | null>(null);

  protected readonly historial = signal<HistorialSolicitudResponse[]>([]);
  protected readonly historialLoading = signal(false);

  protected readonly activosCatalogo = this.activos.asReadonly();
  protected readonly estadosCatalogo = this.estados.asReadonly();

  protected readonly views = computed<SolicitudView[]>(() =>
    this.solicitudes().map((raw) => ({
      raw,
      estadoNombre: this.buscarNombre(this.estados(), raw.id_estado, (e) => e.id_estado, (e) => e.nombre, 'Desconocido'),
      activoNombre: this.nombreActivo(raw.id_activo),
      especialidadNombre: this.buscarNombre(
        this.especialidades(),
        raw.id_especialidad,
        (e) => e.id_especialidad,
        (e) => e.nombre,
        '—',
      ),
      usuarioNombre: this.nombreUsuario(raw.id_usuario),
    })),
  );

  protected readonly kanbanColumns = computed<KanbanColumn[]>(() => {
    const agrupado = new Map<string, SolicitudView[]>();
    for (const view of this.views()) {
      const lista = agrupado.get(view.raw.id_estado) ?? [];
      lista.push(view);
      agrupado.set(view.raw.id_estado, lista);
    }
    return [...this.estadosSolicitud()]
      .sort((a, b) => estadoOrderRank(a.nombre) - estadoOrderRank(b.nombre))
      .map((estado) => ({
        estadoId: estado.id_estado,
        estadoNombre: estado.nombre,
        items: agrupado.get(estado.id_estado) ?? [],
        transicionable: esEstadoEditablePorPut(estado.nombre),
      }));
  });

  protected readonly dialogView = computed<SolicitudView | null>(() => {
    const target = this.dialogTarget();
    if (!target) return null;
    return this.views().find((view) => view.raw.id_solicitud === target.id_solicitud) ?? null;
  });

  // "Finalizado" ya no es editable por PUT (ver solicitud-estados.config.ts):
  // una vez ahí, el formulario de edición se oculta a favor de la ficha de
  // solo lectura, y "Generar Orden" deja de ofrecerse (ya se generó).
  protected readonly dialogEstaFinalizada = computed(() => {
    const view = this.dialogView();
    return !!view && !esEstadoEditablePorPut(view.estadoNombre);
  });

  constructor() {
    this.loadAll();

    effect(() => {
      if (!this.loading() && !this.estadosDisponibles() && this.viewMode() === 'kanban') {
        this.viewMode.set('tabla');
      }
    });
  }

  protected reload(): void {
    this.loadAll();
  }

  protected openCreate(): void {
    this.dialogMode.set('create');
    this.dialogTarget.set(null);
  }

  protected openEdit(view: SolicitudView): void {
    this.dialogMode.set('edit');
    this.dialogTarget.set(view.raw);
    this.loadHistorial(view.raw.id_solicitud);
  }

  protected closeDialog(): void {
    this.dialogMode.set(null);
    this.dialogTarget.set(null);
    this.historial.set([]);
  }

  protected handleFormSubmit(request: SolicitudRequest): void {
    const currentUser = this.authService.user();
    if (!currentUser) return;

    this.formSubmitting.set(true);

    if (this.dialogMode() === 'create') {
      const payload: SolicitudRequest = { ...request, id_usuario: currentUser.id };
      this.solicitudService.crear(payload).subscribe({
        next: (creada) => {
          this.solicitudes.update((lista) => [creada, ...lista]);
          this.formSubmitting.set(false);
          this.closeDialog();
          this.notifications.success(`Solicitud ${creada.numeroSolicitud} creada correctamente.`);
        },
        error: () => this.formSubmitting.set(false),
      });
      return;
    }

    const original = this.dialogTarget();
    if (this.dialogMode() === 'edit' && original) {
      const payload: SolicitudRequest = { ...request, id_usuario: original.id_usuario };
      this.solicitudService.editar(original.id_solicitud, payload).subscribe({
        next: (actualizada) => {
          this.solicitudes.update((lista) =>
            lista.map((s) => (s.id_solicitud === actualizada.id_solicitud ? actualizada : s)),
          );
          this.dialogTarget.set(actualizada);
          this.formSubmitting.set(false);
          this.notifications.success('Solicitud actualizada correctamente.');
        },
        error: () => this.formSubmitting.set(false),
      });
    }
  }

  protected requestDelete(solicitud: SolicitudResponse): void {
    this.confirmTarget.set(solicitud);
  }

  protected cancelDelete(): void {
    this.confirmTarget.set(null);
  }

  protected confirmDeleteAction(): void {
    const target = this.confirmTarget();
    if (!target) return;

    this.deleteSubmitting.set(true);
    this.solicitudService.eliminar(target.id_solicitud).subscribe({
      next: () => {
        this.solicitudes.update((lista) => lista.filter((s) => s.id_solicitud !== target.id_solicitud));
        this.deleteSubmitting.set(false);
        this.confirmTarget.set(null);
        if (this.dialogTarget()?.id_solicitud === target.id_solicitud) {
          this.closeDialog();
        }
        this.notifications.success('Solicitud eliminada correctamente.');
      },
      error: () => this.deleteSubmitting.set(false),
    });
  }

  /** Único camino de UI hacia "Finalizado" — reemplaza el arrastre a esa
   * columna (bloqueado en el Kanban) y la opción en el <select> de edición
   * (excluida en Formulario). */
  protected requestGenerarOrden(solicitud: SolicitudResponse): void {
    this.generarOrdenTarget.set(solicitud);
    this.generarOrdenError.set(null);
  }

  protected cancelGenerarOrden(): void {
    if (this.generarOrdenSubmitting()) return;
    this.generarOrdenTarget.set(null);
    this.generarOrdenError.set(null);
  }

  protected confirmGenerarOrdenAction(): void {
    const target = this.generarOrdenTarget();
    if (!target) return;

    // Generar la OT ya no finaliza la Solicitud: el backend la mueve a "En
    // progreso" (SolicitudServiceImpl.generarOrdenDesdeSolicitud) y solo
    // llega a "Finalizado" cuando la Orden se cierra.
    const estadoEnProgreso = this.estados().find((e) => e.nombre.toLowerCase() === 'en progreso');

    this.generarOrdenSubmitting.set(true);
    this.generarOrdenError.set(null);
    this.solicitudService.generarOrden(target.id_solicitud, {}).subscribe({
      next: (orden) => {
        if (estadoEnProgreso) {
          const actualizada: SolicitudResponse = { ...target, id_estado: estadoEnProgreso.id_estado };
          this.solicitudes.update((lista) =>
            lista.map((s) => (s.id_solicitud === target.id_solicitud ? actualizada : s)),
          );
          if (this.dialogTarget()?.id_solicitud === target.id_solicitud) {
            this.dialogTarget.set(actualizada);
          }
        }
        this.generarOrdenSubmitting.set(false);
        this.generarOrdenTarget.set(null);
        this.notifications.success(`Orden de trabajo ${orden.numeroOrden} generada correctamente.`);
      },
      error: (error: unknown) => {
        this.generarOrdenSubmitting.set(false);
        // El toast global ya muestra este mismo mensaje; se repite acá para
        // que quede visible dentro del propio diálogo de confirmación en
        // vez de depender solo de un toast que puede pasar desapercibido.
        this.generarOrdenError.set(extractApiErrorMessage(error));
      },
    });
  }

  protected handleMoved({ item, estadoDestinoId }: SolicitudMovida): void {
    const estadoAnteriorId = item.raw.id_estado;
    if (estadoAnteriorId === estadoDestinoId) return;

    this.solicitudes.update((lista) =>
      lista.map((s) => (s.id_solicitud === item.raw.id_solicitud ? { ...s, id_estado: estadoDestinoId } : s)),
    );

    const payload: SolicitudRequest = {
      id_usuario: item.raw.id_usuario,
      id_activo: item.raw.id_activo,
      id_estado: estadoDestinoId,
      id_especialidad: item.raw.id_especialidad,
      prioridad: item.raw.prioridad,
      descripcion: item.raw.descripcion,
      url_adjunto: item.raw.url_adjunto,
    };

    this.solicitudService.editar(item.raw.id_solicitud, payload).subscribe({
      next: () => {
        this.notifications.success('Estado actualizado.');
        this.solicitudService
          .registrarHistorial({
            id_solicitud: item.raw.id_solicitud,
            id_estado_anterior: estadoAnteriorId,
            id_estado_nuevo: estadoDestinoId,
          })
          .subscribe({ error: () => undefined });
      },
      error: () => {
        this.solicitudes.update((lista) =>
          lista.map((s) => (s.id_solicitud === item.raw.id_solicitud ? { ...s, id_estado: estadoAnteriorId } : s)),
        );
      },
    });
  }

  protected estadoNombreHistorial(idEstado: string): string {
    return this.buscarNombre(this.estados(), idEstado, (e) => e.id_estado, (e) => e.nombre, 'Desconocido');
  }

  private loadHistorial(idSolicitud: string): void {
    if (!this.canReadHistorial) return;
    this.historialLoading.set(true);
    this.solicitudService.listarHistorial(idSolicitud).subscribe({
      next: (registros) => {
        this.historial.set(
          [...registros].sort((a, b) => b.fecha.localeCompare(a.fecha)),
        );
        this.historialLoading.set(false);
      },
      error: () => this.historialLoading.set(false),
    });
  }

  private loadAll(): void {
    this.loading.set(true);
    this.loadError.set(null);

    forkJoin({
      solicitudes: this.solicitudService.listar(),
      estados: this.catalogoService.getEstados(),
      activos: this.catalogoService.getActivos(),
      especialidades: this.catalogoService.getEspecialidades(),
      usuarios: this.catalogoService.getUsuarios(),
    }).subscribe({
      next: ({ solicitudes, estados, activos, especialidades, usuarios }) => {
        this.solicitudes.set(solicitudes);
        this.estados.set(estados);
        this.activos.set(activos);
        this.especialidades.set(especialidades);
        this.usuarios.set(usuarios);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('No se pudieron cargar las solicitudes.');
        this.loading.set(false);
      },
    });
  }

  private nombreActivo(id: string): string {
    const activo = this.activos().find((item) => item.id_activo === id);
    return activo ? `${activo.codigo} · ${activo.nombre}` : '—';
  }

  private nombreUsuario(id: string): string {
    const usuario = this.usuarios().find((item) => item.id_usuario === id);
    return usuario ? `${usuario.nombres} ${usuario.apellidos}` : '—';
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
