import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideCircleAlert, LucideInbox, LucideLayoutGrid, LucidePlus, LucideTable } from '@lucide/angular';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import { ActivoCatalogo, EspecialidadCatalogo, EstadoCatalogo } from '../../../core/models/catalogo.model';
import {
  GenerarRequerimientoRequest,
  HistorialSolicitudResponse,
  SolicitudRequest,
  SolicitudResponse,
} from '../../../core/models/solicitud.model';
import { ArchivoService } from '../../../core/services/archivo.service';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { SolicitudService } from '../../../core/services/solicitud.service';
import { Button } from '../../../shared/components/button/button';
import { Dialog } from '../../../shared/components/dialog/dialog';
import { Select, SelectOption } from '../../../shared/components/select/select';
import { Spinner } from '../../../shared/components/spinner/spinner';
import { NotificationService } from '../../../shared/services/notification.service';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';
import { estadoOrderRank } from '../../../shared/utils/estado-order.util';
import { Formulario, SolicitudFormSubmit } from './components/formulario/formulario';
import { Kanban, SolicitudMovida } from './components/kanban/kanban';
import { Tabla } from './components/tabla/tabla';
import {
  esEstadoEditablePorPut,
  esEstadoEnRevision,
  esEstadoFinalizado,
  esEstadoKanbanDeSolicitud,
  esEstadoPendiente,
  esEstadoRechazado,
  esEstadoValidoDeSolicitud,
} from './solicitud-estados.config';
import { KanbanColumn, SolicitudView } from './solicitud-view.model';

type ViewMode = 'kanban' | 'tabla';
type DialogMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-solicitudes',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    Select,
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
  private readonly archivoService = inject(ArchivoService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly notifications = inject(NotificationService);

  protected readonly canCreate = this.authService.hasPermission('solicitud.create');
  protected readonly canUpdate = this.authService.hasPermission('solicitud.update');
  protected readonly canDelete = this.authService.hasPermission('solicitud.delete');
  protected readonly canGenerarOrden = this.authService.hasPermission('solicitud.generar_orden');
  protected readonly canGenerarRequerimiento = this.authService.hasPermission('solicitud.generar_requerimiento');
  protected readonly canReadHistorial = this.authService.hasPermission('historial_solicitud.read');

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);

  private readonly solicitudes = signal<SolicitudResponse[]>([]);
  private readonly estados = signal<EstadoCatalogo[]>([]);
  private readonly activos = signal<ActivoCatalogo[]>([]);
  private readonly especialidades = signal<EspecialidadCatalogo[]>([]);

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

  // Al despachar (OT o RQ) el Despachador confirma o cambia la especialidad
  // entre las 5 del catálogo, precargada con la del activo (PRD D16/D22). La
  // OT entra a la cola de esa especialidad: no se elige persona (D3).
  protected readonly generarOrdenTarget = signal<SolicitudResponse | null>(null);
  protected readonly generarOrdenEspecialidad = signal('');
  protected readonly generarOrdenSubmitting = signal(false);
  protected readonly generarOrdenError = signal<string | null>(null);

  // Generar Requerimiento desde una Solicitud "fuera de contrato"
  // (Requerimiento.solicitud es una relación real — ver
  // SolicitudServiceImpl.generarRequerimientoDesdeSolicitud). La descripción
  // se precarga con la de la Solicitud + nota de origen (nunca en blanco) y
  // queda editable para que el usuario la revise antes de confirmar.
  protected readonly generarRequerimientoTarget = signal<SolicitudResponse | null>(null);
  protected readonly generarRequerimientoDescripcion = signal('');
  protected readonly generarRequerimientoEspecialidad = signal('');
  protected readonly generarRequerimientoSubmitting = signal(false);
  protected readonly generarRequerimientoError = signal<string | null>(null);

  // Primero las especialidades del activo de la Solicitud en despacho; luego
  // el resto del catálogo (el Despachador puede elegir cualquiera de las 5).
  protected readonly especialidadOptions = computed<SelectOption[]>(() => {
    const target = this.generarOrdenTarget() ?? this.generarRequerimientoTarget();
    const activo = target ? this.activos().find((a) => a.id_activo === target.id_activo) : undefined;
    const delActivo = new Set(activo?.ids_especialidad ?? []);
    const opcion = (e: EspecialidadCatalogo) => ({
      value: e.id_especialidad,
      label: delActivo.has(e.id_especialidad) ? `${e.nombre} · del activo` : e.nombre,
    });
    return [
      ...this.especialidades().filter((e) => delActivo.has(e.id_especialidad)).map(opcion),
      ...this.especialidades().filter((e) => !delActivo.has(e.id_especialidad)).map(opcion),
    ];
  });

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
      usuarioNombre: raw.nombre_usuario || '—',
    })),
  );

  // Solo las 3 columnas operativas: "En revisión" no tiene columna propia
  // (la Solicitud está siendo tratada por el flujo de aprobación del
  // Requerimiento que generó, no por el flujo operativo directo) — sigue
  // siendo un estado real y se ve igual en la Tabla (que no usa este filtro).
  protected readonly kanbanColumns = computed<KanbanColumn[]>(() => {
    const agrupado = new Map<string, SolicitudView[]>();
    for (const view of this.views()) {
      const lista = agrupado.get(view.raw.id_estado) ?? [];
      lista.push(view);
      agrupado.set(view.raw.id_estado, lista);
    }
    return this.estadosSolicitud()
      .filter((estado) => esEstadoKanbanDeSolicitud(estado.nombre))
      .sort((a, b) => estadoOrderRank(a.nombre) - estadoOrderRank(b.nombre))
      .map((estado) => ({
        estadoId: estado.id_estado,
        estadoNombre: estado.nombre,
        items: esEstadoPendiente(estado.nombre)
          ? this.ordenarPorVencimiento(agrupado.get(estado.id_estado) ?? [])
          : (agrupado.get(estado.id_estado) ?? []),
        transicionable: esEstadoEditablePorPut(estado.nombre),
      }));
  });

  protected readonly dialogView = computed<SolicitudView | null>(() => {
    const target = this.dialogTarget();
    if (!target) return null;
    return this.views().find((view) => view.raw.id_solicitud === target.id_solicitud) ?? null;
  });

  // Comparación directa por nombre de estado — NO por "editable por PUT"
  // (ese conjunto quedó vacío para Solicitud, ver solicitud-estados.config.ts,
  // y usarlo como proxy de "finalizada" hacía que CUALQUIER estado, incluido
  // Pendiente, entrara en la rama de "finalizada").
  protected readonly dialogEstaFinalizada = computed(() => {
    const view = this.dialogView();
    return !!view && (esEstadoFinalizado(view.estadoNombre) || esEstadoRechazado(view.estadoNombre));
  });

  protected readonly dialogEstaRechazada = computed(() => {
    const view = this.dialogView();
    return !!view && esEstadoRechazado(view.estadoNombre);
  });

  // El Despachador clasifica una Solicitud Pendiente en una de dos ramas
  // (bajo contrato -> OT, fuera de contrato -> Requerimiento); ambas acciones
  // solo tienen sentido mientras sigue Pendiente — una vez clasificada
  // (En revisión/En progreso) o cerrado el ciclo (Finalizado), ninguna aplica.
  protected readonly dialogPuedeGenerarOrden = computed(() => {
    const view = this.dialogView();
    return !!view && esEstadoPendiente(view.estadoNombre);
  });

  protected readonly dialogPuedeGenerarRequerimiento = computed(() => {
    const view = this.dialogView();
    return !!view && esEstadoPendiente(view.estadoNombre);
  });

  protected readonly dialogEstaEnRevision = computed(() => {
    const view = this.dialogView();
    return !!view && esEstadoEnRevision(view.estadoNombre);
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

  /** El adjunto exige JWT (Authorization header), así que no puede ser un
   * <a href> plano: se descarga vía HttpClient (el interceptor ya adjunta el
   * token) y se abre como blob local. */
  protected verAdjunto(url: string | null): void {
    if (!url) return;
    this.archivoService.descargarBlob(url).subscribe((blob) => {
      const objectUrl = URL.createObjectURL(blob);
      window.open(objectUrl, '_blank');
      setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
    });
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

  protected handleFormSubmit(submission: SolicitudFormSubmit): void {
    const currentUser = this.authService.user();
    if (!currentUser) return;

    this.formSubmitting.set(true);
    const { request, archivo, eliminarArchivo } = submission;

    if (this.dialogMode() === 'create') {
      const payload: SolicitudRequest = { ...request, id_usuario: currentUser.id };
      this.solicitudService.crear(payload).subscribe({
        next: (creada) =>
          this.gestionarArchivoYFinalizar(creada, archivo, eliminarArchivo, (actualizada) => {
            this.solicitudes.update((lista) => [actualizada, ...lista]);
            this.closeDialog();
            this.notifications.success(`Solicitud ${actualizada.numeroSolicitud} creada correctamente.`);
          }),
        error: () => this.formSubmitting.set(false),
      });
      return;
    }

    const original = this.dialogTarget();
    if (this.dialogMode() === 'edit' && original) {
      const payload: SolicitudRequest = { ...request, id_usuario: original.id_usuario };
      this.solicitudService.editar(original.id_solicitud, payload).subscribe({
        next: (actualizada) =>
          this.gestionarArchivoYFinalizar(actualizada, archivo, eliminarArchivo, (final) => {
            this.solicitudes.update((lista) => lista.map((s) => (s.id_solicitud === final.id_solicitud ? final : s)));
            this.dialogTarget.set(final);
            this.notifications.success('Solicitud actualizada correctamente.');
          }),
        error: () => this.formSubmitting.set(false),
      });
    }
  }

  private gestionarArchivoYFinalizar(
    entidad: SolicitudResponse,
    archivo: File | null,
    eliminarArchivo: boolean,
    onFinal: (actualizada: SolicitudResponse) => void,
  ): void {
    const finalizar = (actualizada: SolicitudResponse) => {
      this.formSubmitting.set(false);
      onFinal(actualizada);
    };

    if (archivo) {
      this.archivoService.subir<SolicitudResponse>('solicitudes', entidad.id_solicitud, archivo).subscribe({
        next: finalizar,
        error: () => {
          this.notifications.error('La solicitud se guardó, pero no se pudo subir el adjunto.');
          finalizar(entidad);
        },
      });
      return;
    }

    if (eliminarArchivo) {
      this.archivoService.eliminar<SolicitudResponse>('solicitudes', entidad.id_solicitud).subscribe({
        next: finalizar,
        error: () => finalizar(entidad),
      });
      return;
    }

    finalizar(entidad);
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
    this.generarOrdenEspecialidad.set(solicitud.id_especialidad);
    this.generarOrdenError.set(null);
  }

  protected cancelGenerarOrden(): void {
    if (this.generarOrdenSubmitting()) return;
    this.generarOrdenTarget.set(null);
    this.generarOrdenError.set(null);
  }

  protected confirmGenerarOrdenAction(): void {
    const target = this.generarOrdenTarget();
    const especialidad = this.generarOrdenEspecialidad();
    if (!target || !especialidad) return;

    // Generar la OT ya no finaliza la Solicitud: el backend la mueve a "En
    // progreso" (SolicitudServiceImpl.generarOrdenDesdeSolicitud) y solo
    // llega a "Finalizado" cuando la Orden se cierra.
    const estadoEnProgreso = this.estados().find((e) => e.nombre.toLowerCase() === 'en progreso');

    this.generarOrdenSubmitting.set(true);
    this.generarOrdenError.set(null);
    this.solicitudService.generarOrden(target.id_solicitud, { id_especialidad: especialidad }).subscribe({
      next: (orden) => {
        if (estadoEnProgreso) {
          const actualizada: SolicitudResponse = {
            ...target,
            id_estado: estadoEnProgreso.id_estado,
            id_especialidad: especialidad,
          };
          this.solicitudes.update((lista) =>
            lista.map((s) => (s.id_solicitud === target.id_solicitud ? actualizada : s)),
          );
          if (this.dialogTarget()?.id_solicitud === target.id_solicitud) {
            this.dialogTarget.set(actualizada);
          }
        }
        this.generarOrdenSubmitting.set(false);
        this.generarOrdenTarget.set(null);
        this.notifications.success(
          `Orden de trabajo ${orden.numeroOrden} generada en la cola de ${this.nombreEspecialidad(especialidad)}.`,
        );
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

  protected requestGenerarRequerimiento(solicitud: SolicitudResponse): void {
    this.generarRequerimientoTarget.set(solicitud);
    // Precarga editable: mismo texto que generaría el backend por defecto
    // (descripción original + nota de origen), para que el usuario nunca
    // tenga que volver a escribirla, pero pueda revisarla/ajustarla antes de
    // confirmar (sección 6 del pedido). Si la edita, se envía tal cual; si no
    // la toca, es idéntica a lo que el backend habría generado solo.
    this.generarRequerimientoDescripcion.set(
      `${solicitud.descripcion}\n\nRequerimiento generado a partir de la Solicitud ${solicitud.numeroSolicitud}.`,
    );
    this.generarRequerimientoEspecialidad.set(solicitud.id_especialidad);
    this.generarRequerimientoError.set(null);
  }

  protected cancelGenerarRequerimiento(): void {
    if (this.generarRequerimientoSubmitting()) return;
    this.generarRequerimientoTarget.set(null);
    this.generarRequerimientoError.set(null);
  }

  protected confirmGenerarRequerimientoAction(): void {
    const target = this.generarRequerimientoTarget();
    if (!target) return;

    const request: GenerarRequerimientoRequest = {
      descripcion: this.generarRequerimientoDescripcion().trim(),
      id_especialidad: this.generarRequerimientoEspecialidad() || undefined,
    };

    this.generarRequerimientoSubmitting.set(true);
    this.generarRequerimientoError.set(null);
    this.solicitudService.generarRequerimiento(target.id_solicitud, request).subscribe({
      next: (requerimiento) => {
        const estadoEnRevision = this.estados().find((e) => e.nombre.toLowerCase() === 'en revisión');
        if (estadoEnRevision) {
          const actualizada: SolicitudResponse = {
            ...target,
            id_estado: estadoEnRevision.id_estado,
            id_especialidad: requerimiento.id_especialidad,
          };
          this.solicitudes.update((lista) =>
            lista.map((s) => (s.id_solicitud === target.id_solicitud ? actualizada : s)),
          );
          if (this.dialogTarget()?.id_solicitud === target.id_solicitud) {
            this.dialogTarget.set(actualizada);
          }
        }
        this.generarRequerimientoSubmitting.set(false);
        this.generarRequerimientoTarget.set(null);
        this.notifications.success(`Requerimiento ${requerimiento.numeroRequerimiento} generado correctamente.`);
      },
      error: (error: unknown) => {
        this.generarRequerimientoSubmitting.set(false);
        this.generarRequerimientoError.set(extractApiErrorMessage(error));
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
    }).subscribe({
      next: ({ solicitudes, estados, activos, especialidades }) => {
        this.solicitudes.set(solicitudes);
        this.estados.set(estados);
        this.activos.set(activos);
        this.especialidades.set(especialidades);
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

  private nombreEspecialidad(id: string): string {
    return this.buscarNombre(this.especialidades(), id, (e) => e.id_especialidad, (e) => e.nombre, 'la especialidad');
  }

  /** Bandeja del Despachador: primero las que vencen antes (SLA de despacho). */
  private ordenarPorVencimiento(items: SolicitudView[]): SolicitudView[] {
    return [...items].sort((a, b) =>
      (a.raw.fecha_limite_despacho ?? '').localeCompare(b.raw.fecha_limite_despacho ?? ''),
    );
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
