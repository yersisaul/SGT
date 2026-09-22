import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideCircleAlert, LucideInbox, LucideLayoutGrid, LucideLock, LucideTable } from '@lucide/angular';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import { EspecialidadCatalogo, EstadoCatalogo, UsuarioCatalogo } from '../../../core/models/catalogo.model';
import {
  HistorialOrdenResponse,
  OrdenRequest,
  OrdenResponse,
  ReasignarOrdenRequest,
} from '../../../core/models/orden.model';
import { ArchivoService } from '../../../core/services/archivo.service';
import { CatalogoService } from '../../../core/services/catalogo.service';
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
import { Kanban, OrdenMovida } from './components/kanban/kanban';
import { Tabla } from './components/tabla/tabla';
import { esEstadoEditablePorPut, esEstadoValidoDeOrden } from './orden-estados.config';
import { OrdenKanbanColumn, OrdenView, OrigenContexto } from './orden-view.model';

type ViewMode = 'kanban' | 'tabla';

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
  private readonly catalogoService = inject(CatalogoService);
  private readonly notifications = inject(NotificationService);

  protected readonly canUpdate = this.authService.hasPermission('orden.update');
  protected readonly canCerrar = this.authService.hasPermission('orden.cerrar');
  protected readonly canDelete = this.authService.hasPermission('orden.delete');
  protected readonly canReasignar = this.authService.hasPermission('orden.reasignar');
  protected readonly canReadHistorial = this.authService.hasPermission('historial_orden.read');
  protected readonly canReadSolicitud = this.authService.hasPermission('solicitud.read');
  protected readonly canReadRequerimiento = this.authService.hasPermission('requerimiento.read');

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);

  private readonly ordenes = signal<OrdenResponse[]>([]);
  private readonly estados = signal<EstadoCatalogo[]>([]);
  private readonly especialidades = signal<EspecialidadCatalogo[]>([]);
  private readonly usuarios = signal<UsuarioCatalogo[]>([]);
  private readonly usuariosOperaciones = signal<UsuarioCatalogo[]>([]);

  protected readonly viewMode = signal<ViewMode>('kanban');

  // Estados válidos para Orden del catálogo compartido /api/estados (también
  // sirve a Solicitud y Requerimiento). Ver orden-estados.config.ts.
  protected readonly estadosOrden = computed(() => this.estados().filter((e) => esEstadoValidoDeOrden(e.nombre)));
  protected readonly estadosDisponibles = computed(() => this.estadosOrden().length > 0);

  protected readonly dialogTarget = signal<OrdenResponse | null>(null);
  protected readonly ejecutarSubmitting = signal(false);

  protected readonly confirmTarget = signal<OrdenResponse | null>(null);
  protected readonly deleteSubmitting = signal(false);

  protected readonly cerrarTarget = signal<OrdenResponse | null>(null);
  protected readonly cerrarComentario = signal('');
  protected readonly cerrarSubmitting = signal(false);
  protected readonly cerrarError = signal<string | null>(null);

  protected readonly reasignarTarget = signal<OrdenResponse | null>(null);
  protected readonly reasignarSeleccionado = signal('');
  protected readonly reasignarComentario = signal('');
  protected readonly reasignarSubmitting = signal(false);
  protected readonly reasignarError = signal<string | null>(null);

  protected readonly historial = signal<HistorialOrdenResponse[]>([]);
  protected readonly historialLoading = signal(false);

  protected readonly origen = signal<OrigenContexto>(null);
  protected readonly origenLoading = signal(false);

  protected readonly estadosCatalogo = this.estados.asReadonly();
  protected readonly estadoBadgeVariant = estadoBadgeVariant;

  protected readonly views = computed<OrdenView[]>(() =>
    this.ordenes().map((raw) => ({
      raw,
      estadoNombre: this.buscarNombre(this.estados(), raw.id_estado, (e) => e.id_estado, (e) => e.nombre, 'Desconocido'),
      especialidadNombre: this.buscarNombre(
        this.especialidades(),
        raw.id_especialidad,
        (e) => e.id_especialidad,
        (e) => e.nombre,
        '—',
      ),
      usuarioNombre: this.nombreEjecutor(raw.id_usuario),
    })),
  );

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
        transicionable: esEstadoEditablePorPut(estado.nombre),
      }));
  });

  protected readonly dialogView = computed<OrdenView | null>(() => {
    const target = this.dialogTarget();
    if (!target) return null;
    return this.views().find((view) => view.raw.id_orden === target.id_orden) ?? null;
  });

  protected readonly dialogEstaCerrada = computed(() => !!this.dialogTarget()?.fecha_cierre);

  // Reasignar es una capacidad de recurso, no solo de permiso (CLAUDE.md
  // 5.5): orden.reasignar habilita el intento, pero solo puede ejecutarlo el
  // ejecutor actualmente asignado o un Administrador — el backend vuelve a
  // validar esto igual, esto es solo la curación de UI.
  protected readonly dialogPuedeReasignar = computed(() => {
    if (!this.canReasignar) return false;
    const target = this.dialogTarget();
    const user = this.authService.user();
    if (!target || !user) return false;
    return target.id_usuario === user.id || user.rol === 'Administrador';
  });

  protected readonly ejecutorOptions = computed<SelectOption[]>(() => {
    const actual = this.reasignarTarget()?.id_usuario;
    return this.usuariosOperaciones()
      .filter((u) => u.id_usuario !== actual)
      .map((u) => ({ value: u.id_usuario, label: `${u.nombres} ${u.apellidos}` }));
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

  protected openDetail(view: OrdenView): void {
    this.dialogTarget.set(view.raw);
    this.loadHistorial(view.raw.id_orden);
    this.loadOrigen(view.raw);
  }

  protected closeDialog(): void {
    this.dialogTarget.set(null);
    this.historial.set([]);
    this.origen.set(null);
  }

  protected handleEjecutar(resultado: EjecutarResultado): void {
    const original = this.dialogTarget();
    if (!original) return;

    this.ejecutarSubmitting.set(true);
    const payload: OrdenRequest = {
      // OrdenResponse no expone id_usuario; el backend lo ignora en el PUT
      // de todas formas (ver OrdenRequest en orden.model.ts).
      id_usuario: '',
      id_estado: resultado.id_estado,
      id_especialidad: original.id_especialidad,
      id_solicitud: original.id_solicitud,
      id_requerimiento: original.id_requerimiento,
    };
    this.ordenService.editar(original.id_orden, payload).subscribe({
      next: (actualizada) =>
        this.gestionarArchivoYFinalizar(actualizada, resultado.archivo, resultado.eliminarArchivo, (final) => {
          this.ordenes.update((lista) => lista.map((o) => (o.id_orden === final.id_orden ? final : o)));
          this.dialogTarget.set(final);
          this.notifications.success('Orden actualizada correctamente.');
        }),
      error: () => this.ejecutarSubmitting.set(false),
    });
  }

  private gestionarArchivoYFinalizar(
    entidad: OrdenResponse,
    archivo: File | null,
    eliminarArchivo: boolean,
    onFinal: (actualizada: OrdenResponse) => void,
  ): void {
    const finalizar = (actualizada: OrdenResponse) => {
      this.ejecutarSubmitting.set(false);
      onFinal(actualizada);
    };

    if (archivo) {
      this.archivoService.subir<OrdenResponse>('ordenes', entidad.id_orden, archivo).subscribe({
        next: finalizar,
        error: () => {
          this.notifications.error('La orden se actualizó, pero no se pudo subir el informe técnico.');
          finalizar(entidad);
        },
      });
      return;
    }

    if (eliminarArchivo) {
      this.archivoService.eliminar<OrdenResponse>('ordenes', entidad.id_orden).subscribe({
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
        this.ordenes.update((lista) => lista.map((o) => (o.id_orden === cerrada.id_orden ? cerrada : o)));
        if (this.dialogTarget()?.id_orden === cerrada.id_orden) {
          this.dialogTarget.set(cerrada);
          this.loadHistorial(cerrada.id_orden);
          // Cerrar la Orden puede finalizar la Solicitud asociada
          // (OrdenServiceImpl.cerrarOrden) — refrescar el origen para que el
          // panel de trazabilidad muestre su estado real, no el cacheado al
          // abrir el detalle.
          this.loadOrigen(cerrada);
        }
        this.cerrarSubmitting.set(false);
        this.cerrarTarget.set(null);
        this.notifications.success(`Orden ${cerrada.numeroOrden} cerrada correctamente.`);
      },
      error: (error: unknown) => {
        this.cerrarSubmitting.set(false);
        this.cerrarError.set(extractApiErrorMessage(error));
      },
    });
  }

  protected requestReasignar(orden: OrdenResponse): void {
    this.reasignarTarget.set(orden);
    this.reasignarSeleccionado.set('');
    this.reasignarComentario.set('');
    this.reasignarError.set(null);
  }

  protected cancelReasignar(): void {
    if (this.reasignarSubmitting()) return;
    this.reasignarTarget.set(null);
    this.reasignarError.set(null);
  }

  protected confirmReasignarAction(): void {
    const target = this.reasignarTarget();
    const nuevoEjecutor = this.reasignarSeleccionado();
    if (!target || !nuevoEjecutor) return;

    const request: ReasignarOrdenRequest = {
      id_usuario_nuevo: nuevoEjecutor,
      comentario: this.reasignarComentario().trim() || undefined,
    };

    this.reasignarSubmitting.set(true);
    this.reasignarError.set(null);
    this.ordenService.reasignar(target.id_orden, request).subscribe({
      next: (actualizada) => {
        this.ordenes.update((lista) => lista.map((o) => (o.id_orden === actualizada.id_orden ? actualizada : o)));
        if (this.dialogTarget()?.id_orden === actualizada.id_orden) {
          this.dialogTarget.set(actualizada);
          this.loadHistorial(actualizada.id_orden);
        }
        this.reasignarSubmitting.set(false);
        this.reasignarTarget.set(null);
        this.notifications.success(`Orden ${actualizada.numeroOrden} reasignada correctamente.`);
      },
      error: (error: unknown) => {
        this.reasignarSubmitting.set(false);
        this.reasignarError.set(extractApiErrorMessage(error));
      },
    });
  }

  protected handleMoved({ item, estadoDestinoId }: OrdenMovida): void {
    const estadoAnteriorId = item.raw.id_estado;
    if (estadoAnteriorId === estadoDestinoId) return;

    this.ordenes.update((lista) =>
      lista.map((o) => (o.id_orden === item.raw.id_orden ? { ...o, id_estado: estadoDestinoId } : o)),
    );

    const payload: OrdenRequest = {
      id_usuario: '',
      id_estado: estadoDestinoId,
      id_especialidad: item.raw.id_especialidad,
      id_solicitud: item.raw.id_solicitud,
      id_requerimiento: item.raw.id_requerimiento,
    };

    this.ordenService.editar(item.raw.id_orden, payload).subscribe({
      next: () => this.notifications.success('Estado actualizado.'),
      error: () => {
        this.ordenes.update((lista) =>
          lista.map((o) => (o.id_orden === item.raw.id_orden ? { ...o, id_estado: estadoAnteriorId } : o)),
        );
      },
    });
  }

  protected estadoNombreHistorial(idEstado: string): string {
    return this.buscarNombre(this.estados(), idEstado, (e) => e.id_estado, (e) => e.nombre, 'Desconocido');
  }

  /** HistorialOrdenResponse trae id_usuario: quién realizó cada transición
   * registrada (puede ser cualquier rol, no solo Operaciones). */
  protected nombreUsuarioPorId(idUsuario: string): string {
    const usuario = this.usuarios().find((item) => item.id_usuario === idUsuario);
    return usuario ? `${usuario.nombres} ${usuario.apellidos}` : '—';
  }

  /** El ejecutor (Orden.usuario) siempre es de rol Operaciones; se resuelve
   * contra /usuarios/operaciones porque el catálogo general de usuarios
   * requiere usuario.read, que Operaciones no tiene. */
  private nombreEjecutor(idUsuario: string): string {
    const usuario = this.usuariosOperaciones().find((item) => item.id_usuario === idUsuario);
    return usuario ? `${usuario.nombres} ${usuario.apellidos}` : '—';
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

  /** Origen (instrucción 5/6): distingue Solicitud vs Requerimiento según
   * cuál de id_solicitud/id_requerimiento viene con valor. Se resuelve bajo
   * demanda al abrir el detalle, no para cada tarjeta de la bandeja. */
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
      usuarios: this.catalogoService.getUsuarios(),
      usuariosOperaciones: this.catalogoService.getUsuariosOperaciones(),
    }).subscribe({
      next: ({ ordenes, estados, especialidades, usuarios, usuariosOperaciones }) => {
        this.ordenes.set(ordenes);
        this.estados.set(estados);
        this.especialidades.set(especialidades);
        this.usuarios.set(usuarios);
        this.usuariosOperaciones.set(usuariosOperaciones);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('No se pudieron cargar las órdenes.');
        this.loading.set(false);
      },
    });
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
