import { Component, computed, inject, input, output, signal } from '@angular/core';

import { esLecturaBase } from '../../../../../../core/auth/permisos-base';
import { PermisoResponse } from '../../../../../../core/models/permiso.model';
import { RolPermisoResponse } from '../../../../../../core/models/rol-permiso.model';
import { RolPermisoService } from '../../../../../../core/services/rol-permiso.service';
import { Spinner } from '../../../../../../shared/components/spinner/spinner';
import { NotificationService } from '../../../../../../shared/services/notification.service';
import { extractApiErrorMessage } from '../../../../../../shared/utils/api-error.util';

interface PermisoGrupo {
  recurso: string;
  items: PermisoResponse[];
}

/**
 * Checklist de permisos de un Rol, integrado en su drawer de edición (Fase 3:
 * "no crear una pantalla independiente de RolPermiso"). No hay endpoint bulk
 * en RolPermisoController (solo create/read/delete individuales), así que
 * cada checkbox dispara su propia asignación/revocación inmediatamente, con
 * su propio estado de carga — no existe un botón "Guardar" separado.
 */
@Component({
  selector: 'app-rol-permisos',
  imports: [Spinner],
  templateUrl: './permisos.html',
  styleUrl: './permisos.css',
})
export class Permisos {
  private readonly rolPermisoService = inject(RolPermisoService);
  private readonly notifications = inject(NotificationService);

  readonly idRol = input.required<string>();
  readonly catalogoPermisos = input<PermisoResponse[]>([]);
  readonly asignados = input<RolPermisoResponse[]>([]);
  readonly canManage = input(false);

  readonly asignado = output<RolPermisoResponse>();
  readonly revocado = output<string>();

  protected readonly pendientes = signal<Set<string>>(new Set());

  /** Lecturas base: marcadas y bloqueadas, las otorga el backend a todo rol. */
  protected readonly esLecturaBase = esLecturaBase;

  protected readonly grupos = computed<PermisoGrupo[]>(() => {
    const porRecurso = new Map<string, PermisoResponse[]>();
    for (const permiso of this.catalogoPermisos()) {
      const recurso = permiso.codigo.split('.')[0] ?? permiso.codigo;
      const lista = porRecurso.get(recurso) ?? [];
      lista.push(permiso);
      porRecurso.set(recurso, lista);
    }
    return Array.from(porRecurso.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([recurso, items]) => ({ recurso, items: items.sort((a, b) => a.codigo.localeCompare(b.codigo)) }));
  });

  protected asignacionDe(idPermiso: string): RolPermisoResponse | undefined {
    return this.asignados().find((rp) => rp.id_permiso === idPermiso);
  }

  protected estaAsignado(idPermiso: string): boolean {
    return !!this.asignacionDe(idPermiso);
  }

  protected estaPendiente(idPermiso: string): boolean {
    return this.pendientes().has(idPermiso);
  }

  protected toggle(permiso: PermisoResponse, checked: boolean): void {
    if (!this.canManage() || esLecturaBase(permiso.codigo) || this.estaPendiente(permiso.id_permiso)) return;

    this.setPendiente(permiso.id_permiso, true);

    if (checked) {
      this.rolPermisoService.asignar({ id_rol: this.idRol(), id_permiso: permiso.id_permiso }).subscribe({
        next: (creado) => {
          this.setPendiente(permiso.id_permiso, false);
          this.asignado.emit(creado);
        },
        error: (error: unknown) => {
          this.setPendiente(permiso.id_permiso, false);
          this.notifications.error(extractApiErrorMessage(error));
        },
      });
      return;
    }

    const asignacion = this.asignacionDe(permiso.id_permiso);
    if (!asignacion) {
      this.setPendiente(permiso.id_permiso, false);
      return;
    }
    this.rolPermisoService.revocar(asignacion.id_rol_permiso).subscribe({
      next: () => {
        this.setPendiente(permiso.id_permiso, false);
        this.revocado.emit(asignacion.id_rol_permiso);
      },
      error: (error: unknown) => {
        this.setPendiente(permiso.id_permiso, false);
        this.notifications.error(extractApiErrorMessage(error));
      },
    });
  }

  private setPendiente(idPermiso: string, pendiente: boolean): void {
    this.pendientes.update((set) => {
      const copia = new Set(set);
      if (pendiente) {
        copia.add(idPermiso);
      } else {
        copia.delete(idPermiso);
      }
      return copia;
    });
  }
}
