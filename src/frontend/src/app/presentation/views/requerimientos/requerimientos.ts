import { DatePipe, LowerCasePipe } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  LucideCircleAlert,
  LucideCircleCheck,
  LucideCircleX,
  LucideInbox,
  LucideLayoutGrid,
  LucidePlus,
  LucideTable,
  LucideWrench,
} from '../../../shared/icons/lucide-icons';
import { forkJoin, of } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import { AprobacionResponse } from '../../../core/models/aprobacion.model';
import { EspecialidadCatalogo, EstadoCatalogo, UsuarioCatalogo } from '../../../core/models/catalogo.model';
import { HistorialOrdenResponse, OrdenResponse } from '../../../core/models/orden.model';
import {
  HistorialRequerimientoResponse,
  RequerimientoRequest,
  RequerimientoResponse,
} from '../../../core/models/requerimiento.model';
import { SolicitudResponse } from '../../../core/models/solicitud.model';
import { AprobacionService } from '../../../core/services/aprobacion.service';
import { ArchivoService } from '../../../core/services/archivo.service';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { OrdenService } from '../../../core/services/orden.service';
import { RequerimientoService } from '../../../core/services/requerimiento.service';
import { SolicitudService } from '../../../core/services/solicitud.service';
import { Button } from '../../../shared/components/button/button';
import { Dialog } from '../../../shared/components/dialog/dialog';
import { FileUpload } from '../../../shared/components/file-upload/file-upload';
import { Select, SelectOption } from '../../../shared/components/select/select';
import { Spinner } from '../../../shared/components/spinner/spinner';
import { NotificationService } from '../../../shared/services/notification.service';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';
import { estadoOrderRank } from '../../../shared/utils/estado-order.util';
import { Formulario, RequerimientoFormSubmit } from './components/formulario/formulario';
import { Kanban, RequerimientoMovido } from './components/kanban/kanban';
import { Tabla } from './components/tabla/tabla';
import {
  esEstadoAprobado,
  esEstadoConOrden,
  esEstadoDecidido,
  esEstadoEditablePorPut,
  esEstadoPendienteDecision,
  esEstadoKanbanDeRequerimiento,
  esEstadoValidoDeRequerimiento,
} from './requerimiento-estados.config';
import { RequerimientoKanbanColumn, RequerimientoView } from './requerimiento-view.model';

type ViewMode = 'kanban' | 'tabla';
type DialogMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-requerimientos',
  imports: [
    DatePipe,
    LowerCasePipe,
    FormsModule,
    RouterLink,
    Button,
    Dialog,
    Select,
    Spinner,
    Kanban,
    Tabla,
    Formulario,
    FileUpload,
    LucidePlus,
    LucideLayoutGrid,
    LucideTable,
    LucideInbox,
    LucideCircleAlert,
    LucideCircleCheck,
    LucideCircleX,
    LucideWrench,
  ],
  templateUrl: './requerimientos.html',
  styleUrl: './requerimientos.css',
})
export class Requerimientos {
  private readonly authService = inject(AuthService);
  private readonly requerimientoService = inject(RequerimientoService);
  private readonly solicitudService = inject(SolicitudService);
  private readonly aprobacionService = inject(AprobacionService);
  private readonly archivoService = inject(ArchivoService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly ordenService = inject(OrdenService);
  private readonly notifications = inject(NotificationService);

  protected readonly canCreate = this.authService.hasPermission('requerimiento.create');
  protected readonly canUpdate = this.authService.hasPermission('requerimiento.update');
  protected readonly canDelete = this.authService.hasPermission('requerimiento.delete');
  protected readonly canAprobar = this.authService.hasPermission('requerimiento.aprobar');
  protected readonly canGenerarOrden = this.authService.hasPermission('requerimiento.generar_orden');
  protected readonly canReadHistorial = this.authService.hasPermission('historial_requerimiento.read');
  protected readonly canReadAprobaciones = this.authService.hasPermission('aprobacion.read');
  protected readonly canReadOrden = this.authService.hasPermission('orden.read');
  protected readonly canReadHistorialOrden = this.authService.hasPermission('historial_orden.read');
  protected readonly canReadSolicitud = this.authService.hasPermission('solicitud.read');

  // Trazabilidad Requerimiento -> Solicitud de origen (Requerimiento.solicitud,
  // relación real): se resuelve puntualmente al abrir el detalle, no con un
  // listado completo — igual criterio que ordenAsociada más abajo.
  protected readonly origenSolicitud = signal<SolicitudResponse | null>(null);
  protected readonly origenSolicitudLoading = signal(false);

  // Trazabilidad Requerimiento -> OT (Observación 7): Orden.id_requerimiento
  // es la única relación real; no hay endpoint filtrado, así que se trae la
  // lista completa (requiere orden.read) y se busca la que corresponda. Como
  // mucho hay una por Requerimiento (RequerimientoServiceImpl bloquea
  // generar-orden si ya existe una vía existsByRequerimiento).
  protected readonly ordenAsociada = signal<OrdenResponse | null>(null);
  protected readonly ordenAsociadaLoading = signal(false);
  protected readonly ordenAsociadaSinAcceso = signal(false);
  protected readonly ordenHistorial = signal<HistorialOrdenResponse[]>([]);
  protected readonly ordenHistorialLoading = signal(false);

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);

  private readonly requerimientos = signal<RequerimientoResponse[]>([]);
  private readonly estados = signal<EstadoCatalogo[]>([]);
  private readonly especialidades = signal<EspecialidadCatalogo[]>([]);
  private readonly usuarios = signal<UsuarioCatalogo[]>([]);

  protected readonly viewMode = signal<ViewMode>('kanban');

  // Estados válidos para Requerimiento del catálogo compartido /api/estados
  // (también sirve a Solicitud y Orden). Ver requerimiento-estados.config.ts.
  protected readonly estadosRequerimiento = computed(() =>
    this.estados().filter((e) => esEstadoValidoDeRequerimiento(e.nombre)),
  );
  protected readonly estadosDisponibles = computed(() => this.estadosRequerimiento().length > 0);

  protected readonly dialogMode = signal<DialogMode>(null);
  protected readonly dialogTarget = signal<RequerimientoResponse | null>(null);
  protected readonly formSubmitting = signal(false);

  protected readonly confirmTarget = signal<RequerimientoResponse | null>(null);
  protected readonly deleteSubmitting = signal(false);

  protected readonly aprobacionAccion = signal<{ target: RequerimientoResponse; aprobado: boolean } | null>(null);
  protected readonly aprobacionComentario = signal('');
  protected readonly aprobacionArchivo = signal<File | null>(null);
  protected readonly aprobacionSubmitting = signal(false);
  protected readonly aprobacionError = signal<string | null>(null);

  protected readonly generarOrdenTarget = signal<RequerimientoResponse | null>(null);
  protected readonly generarOrdenSubmitting = signal(false);
  protected readonly generarOrdenError = signal<string | null>(null);

  // Compartido por ambos diálogos de generación de OT (el normal y el
  // posterior a aprobar): solo uno puede estar abierto a la vez. La OT entra
  // a la cola de la especialidad elegida entre las 5 (PRD D16); no se elige
  // persona (D3).
  protected readonly generarOrdenEspecialidad = signal('');
  protected readonly especialidadOptions = computed<SelectOption[]>(() =>
    this.especialidades().map((e) => ({ value: e.id_especialidad, label: e.nombre })),
  );

  // Decisión posterior a aprobar (Observación 6): "generar ahora" ejecuta el
  // mismo endpoint requerimiento.generar_orden directamente, sin abrir un
  // segundo diálogo de confirmación; "generar después" solo cierra y deja
  // visible el botón normal "Generar Orden de Trabajo" en la ficha.
  protected readonly postAprobacionTarget = signal<RequerimientoResponse | null>(null);
  protected readonly postAprobacionSubmitting = signal(false);
  protected readonly postAprobacionError = signal<string | null>(null);

  protected readonly historial = signal<HistorialRequerimientoResponse[]>([]);
  protected readonly historialLoading = signal(false);

  protected readonly aprobaciones = signal<AprobacionResponse[]>([]);
  protected readonly aprobacionesLoading = signal(false);

  protected readonly especialidadesCatalogo = this.especialidades.asReadonly();
  protected readonly estadosCatalogo = this.estados.asReadonly();

  protected readonly views = computed<RequerimientoView[]>(() =>
    this.requerimientos().map((raw) => ({
      raw,
      estadoNombre: this.buscarNombre(this.estados(), raw.id_estado, (e) => e.id_estado, (e) => e.nombre, 'Desconocido'),
      especialidadNombre: this.buscarNombre(
        this.especialidades(),
        raw.id_especialidad,
        (e) => e.id_especialidad,
        (e) => e.nombre,
        '—',
      ),
      creadoPorNombre: raw.nombre_usuario || '—',
    })),
  );

  // Solo las 4 columnas operativas (Observación 3 del pedido): "Aprobado"/
  // "Rechazado" no tienen columna propia en el Kanban, aunque siguen siendo
  // estados reales y se muestran igual en la Tabla (que no depende de este
  // filtro, ver components/tabla/tabla.ts).
  protected readonly kanbanColumns = computed<RequerimientoKanbanColumn[]>(() => {
    const agrupado = new Map<string, RequerimientoView[]>();
    for (const view of this.views()) {
      const lista = agrupado.get(view.raw.id_estado) ?? [];
      lista.push(view);
      agrupado.set(view.raw.id_estado, lista);
    }
    return this.estadosRequerimiento()
      .filter((estado) => esEstadoKanbanDeRequerimiento(estado.nombre))
      .sort((a, b) => estadoOrderRank(a.nombre) - estadoOrderRank(b.nombre))
      .map((estado) => ({
        estadoId: estado.id_estado,
        estadoNombre: estado.nombre,
        items: agrupado.get(estado.id_estado) ?? [],
        transicionable: esEstadoEditablePorPut(estado.nombre),
      }));
  });

  // Para Administrador: cuántos requerimientos esperan su decisión — se
  // destaca arriba de la bandeja para que sean fáciles de identificar.
  protected readonly pendientesDecision = computed(
    () => this.views().filter((v) => esEstadoPendienteDecision(v.estadoNombre)).length,
  );

  protected readonly dialogView = computed<RequerimientoView | null>(() => {
    const target = this.dialogTarget();
    if (!target) return null;
    return this.views().find((view) => view.raw.id_requerimiento === target.id_requerimiento) ?? null;
  });

  protected readonly dialogEstaDecidido = computed(() => {
    const view = this.dialogView();
    return !!view && esEstadoDecidido(view.estadoNombre);
  });

  protected readonly dialogEstaAprobado = computed(() => {
    const view = this.dialogView();
    return !!view && esEstadoAprobado(view.estadoNombre);
  });

  // Aprobar/Rechazar y editar solo aplican mientras espera decisión (Pendiente
  // o En revisión); en En progreso/Finalizado ya fue aprobado y tiene OT.
  protected readonly dialogPendienteDecision = computed(() => {
    const view = this.dialogView();
    return !!view && esEstadoPendienteDecision(view.estadoNombre);
  });

  protected readonly dialogTieneOrden = computed(() => {
    const view = this.dialogView();
    return !!view && esEstadoConOrden(view.estadoNombre);
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

  protected openEdit(view: RequerimientoView): void {
    this.dialogMode.set('edit');
    this.dialogTarget.set(view.raw);
    this.loadHistorial(view.raw.id_requerimiento);
    this.loadAprobaciones(view.raw.id_requerimiento);
    if (esEstadoConOrden(view.estadoNombre)) {
      this.loadOrdenAsociada(view.raw.id_requerimiento);
    } else {
      this.ordenAsociada.set(null);
      this.ordenAsociadaSinAcceso.set(false);
    }
    this.loadOrigenSolicitud(view.raw.id_solicitud);
  }

  protected closeDialog(): void {
    this.dialogMode.set(null);
    this.dialogTarget.set(null);
    this.historial.set([]);
    this.aprobaciones.set([]);
    this.ordenAsociada.set(null);
    this.ordenAsociadaSinAcceso.set(false);
    this.ordenHistorial.set([]);
    this.origenSolicitud.set(null);
  }

  private loadOrigenSolicitud(idSolicitud: string | null): void {
    this.origenSolicitud.set(null);
    if (!idSolicitud || !this.canReadSolicitud) return;
    this.origenSolicitudLoading.set(true);
    this.solicitudService.obtener(idSolicitud).subscribe({
      next: (solicitud) => {
        this.origenSolicitud.set(solicitud);
        this.origenSolicitudLoading.set(false);
      },
      error: () => this.origenSolicitudLoading.set(false),
    });
  }

  private loadOrdenAsociada(idRequerimiento: string): void {
    this.ordenAsociada.set(null);
    this.ordenAsociadaSinAcceso.set(false);
    this.ordenHistorial.set([]);
    if (!this.canReadOrden) {
      this.ordenAsociadaSinAcceso.set(true);
      return;
    }
    this.ordenAsociadaLoading.set(true);
    this.ordenService.listar().subscribe({
      next: (ordenes) => {
        const encontrada = ordenes.find((orden) => orden.id_requerimiento === idRequerimiento) ?? null;
        this.ordenAsociada.set(encontrada);
        this.ordenAsociadaLoading.set(false);
        if (encontrada && this.canReadHistorialOrden) {
          this.ordenHistorialLoading.set(true);
          this.ordenService.listarHistorial(encontrada.id_orden).subscribe({
            next: (registros) => {
              this.ordenHistorial.set([...registros].sort((a, b) => a.fecha.localeCompare(b.fecha)));
              this.ordenHistorialLoading.set(false);
            },
            error: () => this.ordenHistorialLoading.set(false),
          });
        }
      },
      error: () => {
        this.ordenAsociadaSinAcceso.set(true);
        this.ordenAsociadaLoading.set(false);
      },
    });
  }

  protected handleFormSubmit(submission: RequerimientoFormSubmit): void {
    const currentUser = this.authService.user();
    if (!currentUser) return;

    this.formSubmitting.set(true);
    const { request, archivo, eliminarArchivo } = submission;

    if (this.dialogMode() === 'create') {
      const payload: RequerimientoRequest = { ...request, id_usuario: currentUser.id };
      this.requerimientoService.crear(payload).subscribe({
        next: (creado) =>
          this.gestionarArchivoYFinalizar(creado, archivo, eliminarArchivo, (actualizado) => {
            this.requerimientos.update((lista) => [actualizado, ...lista]);
            this.closeDialog();
            this.notifications.success(`Requerimiento ${actualizado.numeroRequerimiento} creado correctamente.`);
          }),
        error: () => this.formSubmitting.set(false),
      });
      return;
    }

    const original = this.dialogTarget();
    if (this.dialogMode() === 'edit' && original) {
      const payload: RequerimientoRequest = { ...request, id_usuario: original.id_usuario };
      this.requerimientoService.editar(original.id_requerimiento, payload).subscribe({
        next: (actualizado) =>
          this.gestionarArchivoYFinalizar(actualizado, archivo, eliminarArchivo, (final) => {
            this.requerimientos.update((lista) =>
              lista.map((r) => (r.id_requerimiento === final.id_requerimiento ? final : r)),
            );
            this.dialogTarget.set(final);
            this.notifications.success('Requerimiento actualizado correctamente.');
          }),
        error: () => this.formSubmitting.set(false),
      });
    }
  }

  private gestionarArchivoYFinalizar(
    entidad: RequerimientoResponse,
    archivo: File | null,
    eliminarArchivo: boolean,
    onFinal: (actualizado: RequerimientoResponse) => void,
  ): void {
    const finalizar = (actualizado: RequerimientoResponse) => {
      this.formSubmitting.set(false);
      onFinal(actualizado);
    };

    if (archivo) {
      this.archivoService.subir<RequerimientoResponse>('requerimientos', entidad.id_requerimiento, archivo).subscribe({
        next: finalizar,
        error: () => {
          this.notifications.error('El requerimiento se guardó, pero no se pudo subir el adjunto.');
          finalizar(entidad);
        },
      });
      return;
    }

    if (eliminarArchivo) {
      this.archivoService.eliminar<RequerimientoResponse>('requerimientos', entidad.id_requerimiento).subscribe({
        next: finalizar,
        error: () => finalizar(entidad),
      });
      return;
    }

    finalizar(entidad);
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

  protected requestDelete(requerimiento: RequerimientoResponse): void {
    this.confirmTarget.set(requerimiento);
  }

  protected cancelDelete(): void {
    this.confirmTarget.set(null);
  }

  protected confirmDeleteAction(): void {
    const target = this.confirmTarget();
    if (!target) return;

    this.deleteSubmitting.set(true);
    this.requerimientoService.eliminar(target.id_requerimiento).subscribe({
      next: () => {
        this.requerimientos.update((lista) => lista.filter((r) => r.id_requerimiento !== target.id_requerimiento));
        this.deleteSubmitting.set(false);
        this.confirmTarget.set(null);
        if (this.dialogTarget()?.id_requerimiento === target.id_requerimiento) {
          this.closeDialog();
        }
        this.notifications.success('Requerimiento eliminado correctamente.');
      },
      error: () => this.deleteSubmitting.set(false),
    });
  }

  /** Aprobar/rechazar: mismo endpoint (POST /aprobaciones), el resultado
   * depende del booleano `aprobado` — no son dos operaciones distintas en
   * el backend, tampoco lo son acá. */
  protected requestAprobacion(target: RequerimientoResponse, aprobado: boolean): void {
    this.aprobacionAccion.set({ target, aprobado });
    this.aprobacionComentario.set('');
    this.aprobacionArchivo.set(null);
    this.aprobacionError.set(null);
  }

  protected cancelAprobacion(): void {
    if (this.aprobacionSubmitting()) return;
    this.aprobacionAccion.set(null);
    this.aprobacionError.set(null);
  }

  protected confirmAprobacionAction(): void {
    const accion = this.aprobacionAccion();
    if (!accion) return;

    this.aprobacionSubmitting.set(true);
    this.aprobacionError.set(null);
    const archivo = this.aprobacionArchivo();
    this.aprobacionService
      .crear({
        id_requerimiento: accion.target.id_requerimiento,
        aprobado: accion.aprobado,
        comentario: this.aprobacionComentario().trim() || null,
      })
      .subscribe({
        next: (aprobacionCreada) => {
          // El presupuesto/documento de respaldo se sube recién con el id de
          // la Aprobacion ya creada; si la subida falla, la aprobación/rechazo ya quedó
          // registrada — no se revierte por eso.
          const actualizadoCallback = (actualizado: RequerimientoResponse) => {
            this.requerimientos.update((lista) =>
              lista.map((r) => (r.id_requerimiento === actualizado.id_requerimiento ? actualizado : r)),
            );
            if (this.dialogTarget()?.id_requerimiento === actualizado.id_requerimiento) {
              this.dialogTarget.set(actualizado);
              this.loadHistorial(actualizado.id_requerimiento);
              this.loadAprobaciones(actualizado.id_requerimiento);
            }
            // El diálogo de decisión "generar ahora/después" solo tiene
            // sentido cuando el propio usuario puede ejecutar generar-orden;
            // si no tiene el permiso, el Requerimiento simplemente queda
            // "Aprobado" para que otro rol continúe más tarde.
            if (accion.aprobado && this.canGenerarOrden) {
              this.postAprobacionError.set(null);
              this.generarOrdenEspecialidad.set(actualizado.id_especialidad);
              this.postAprobacionTarget.set(actualizado);
            }
          };
          const continuar = () => this.finalizarAprobacion(accion, actualizadoCallback);

          if (archivo) {
            this.archivoService.subir('aprobaciones', aprobacionCreada.id_aprobacion, archivo).subscribe({
              next: () => continuar(),
              error: () => {
                this.notifications.error('Se registró la decisión, pero no se pudo subir el presupuesto.');
                continuar();
              },
            });
          } else {
            continuar();
          }
        },
        error: (error: unknown) => {
          this.aprobacionSubmitting.set(false);
          this.aprobacionError.set(extractApiErrorMessage(error));
        },
      });
  }

  private finalizarAprobacion(
    accion: { target: RequerimientoResponse; aprobado: boolean },
    actualizadoCallback: (actualizado: RequerimientoResponse) => void,
  ): void {
    // El estado real lo fija el backend (Aprobado/Rechazado); se refresca el
    // requerimiento puntual para reflejarlo sin recargar toda la bandeja.
    this.requerimientoService.obtener(accion.target.id_requerimiento).subscribe(actualizadoCallback);
    this.aprobacionSubmitting.set(false);
    this.aprobacionAccion.set(null);
    this.notifications.success(accion.aprobado ? 'Requerimiento aprobado.' : 'Requerimiento rechazado.');
  }

  protected requestGenerarOrden(requerimiento: RequerimientoResponse): void {
    this.generarOrdenTarget.set(requerimiento);
    this.generarOrdenEspecialidad.set(requerimiento.id_especialidad);
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

    this.generarOrdenSubmitting.set(true);
    this.generarOrdenError.set(null);
    this.requerimientoService.generarOrden(target.id_requerimiento, { id_especialidad: especialidad }).subscribe({
      next: (orden) => {
        this.generarOrdenSubmitting.set(false);
        this.generarOrdenTarget.set(null);
        this.notifications.success(`Orden de trabajo ${orden.numeroOrden} generada correctamente.`);
        if (this.dialogTarget()?.id_requerimiento === target.id_requerimiento) {
          this.loadOrdenAsociada(target.id_requerimiento);
        }
      },
      error: (error: unknown) => {
        this.generarOrdenSubmitting.set(false);
        this.generarOrdenError.set(extractApiErrorMessage(error));
      },
    });
  }

  protected generarOrdenDespues(): void {
    if (this.postAprobacionSubmitting()) return;
    this.postAprobacionTarget.set(null);
    this.postAprobacionError.set(null);
  }

  protected confirmGenerarOrdenAhora(): void {
    const target = this.postAprobacionTarget();
    const especialidad = this.generarOrdenEspecialidad();
    if (!target || !especialidad) return;

    this.postAprobacionSubmitting.set(true);
    this.postAprobacionError.set(null);
    this.requerimientoService.generarOrden(target.id_requerimiento, { id_especialidad: especialidad }).subscribe({
      next: (orden) => {
        this.postAprobacionSubmitting.set(false);
        this.postAprobacionTarget.set(null);
        this.notifications.success(`Orden de trabajo ${orden.numeroOrden} generada correctamente.`);
        if (this.dialogTarget()?.id_requerimiento === target.id_requerimiento) {
          this.loadOrdenAsociada(target.id_requerimiento);
        }
      },
      error: (error: unknown) => {
        this.postAprobacionSubmitting.set(false);
        // No falsear el éxito: si falla, el diálogo permanece abierto con el
        // motivo real del backend y el Requerimiento sigue "Aprobado".
        this.postAprobacionError.set(extractApiErrorMessage(error));
      },
    });
  }

  protected handleMoved({ item, estadoDestinoId }: RequerimientoMovido): void {
    const estadoAnteriorId = item.raw.id_estado;
    if (estadoAnteriorId === estadoDestinoId) return;

    this.requerimientos.update((lista) =>
      lista.map((r) => (r.id_requerimiento === item.raw.id_requerimiento ? { ...r, id_estado: estadoDestinoId } : r)),
    );

    const payload: RequerimientoRequest = {
      id_usuario: item.raw.id_usuario,
      id_estado: estadoDestinoId,
      id_especialidad: item.raw.id_especialidad,
      descripcion: item.raw.descripcion,
    };

    this.requerimientoService.editar(item.raw.id_requerimiento, payload).subscribe({
      next: () => this.notifications.success('Estado actualizado.'),
      error: () => {
        this.requerimientos.update((lista) =>
          lista.map((r) =>
            r.id_requerimiento === item.raw.id_requerimiento ? { ...r, id_estado: estadoAnteriorId } : r,
          ),
        );
      },
    });
  }

  protected estadoNombreHistorial(idEstado: string): string {
    return this.buscarNombre(this.estados(), idEstado, (e) => e.id_estado, (e) => e.nombre, 'Desconocido');
  }

  protected nombreUsuarioPorId(idUsuario: string): string {
    return this.nombreUsuario(idUsuario);
  }

  private loadHistorial(idRequerimiento: string): void {
    if (!this.canReadHistorial) return;
    this.historialLoading.set(true);
    this.requerimientoService.listarHistorial(idRequerimiento).subscribe({
      next: (registros) => {
        this.historial.set([...registros].sort((a, b) => b.fecha.localeCompare(a.fecha)));
        this.historialLoading.set(false);
      },
      error: () => this.historialLoading.set(false),
    });
  }

  private loadAprobaciones(idRequerimiento: string): void {
    if (!this.canReadAprobaciones) return;
    this.aprobacionesLoading.set(true);
    this.aprobacionService.listarPorRequerimiento(idRequerimiento).subscribe({
      next: (registros) => {
        this.aprobaciones.set([...registros].sort((a, b) => b.fecha_aprobacion.localeCompare(a.fecha_aprobacion)));
        this.aprobacionesLoading.set(false);
      },
      error: () => this.aprobacionesLoading.set(false),
    });
  }

  private loadAll(): void {
    this.loading.set(true);
    this.loadError.set(null);

    forkJoin({
      requerimientos: this.requerimientoService.listar(),
      estados: this.catalogoService.getEstados(),
      especialidades: this.catalogoService.getEspecialidades(),
      // Solo para el autor de cada aprobación (quien tiene aprobacion.read
      // también tiene usuario.read); sin el permiso, un 403 tumbaba la vista.
      usuarios: this.authService.hasPermission('usuario.read') ? this.catalogoService.getUsuarios() : of([]),
    }).subscribe({
      next: ({ requerimientos, estados, especialidades, usuarios }) => {
        this.requerimientos.set(requerimientos);
        this.estados.set(estados);
        this.especialidades.set(especialidades);
        this.usuarios.set(usuarios);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('No se pudieron cargar los requerimientos.');
        this.loading.set(false);
      },
    });
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
